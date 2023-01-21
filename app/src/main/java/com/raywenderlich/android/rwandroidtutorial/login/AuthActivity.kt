package com.raywenderlich.android.rwandroidtutorial.login

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.raywenderlich.android.runtracking.R
import com.raywenderlich.android.runtracking.databinding.ActivityAuthBinding
import com.raywenderlich.android.rwandroidtutorial.login.viewmodel.AuthViewModel
import com.raywenderlich.android.rwandroidtutorial.models.User
import com.raywenderlich.android.rwandroidtutorial.provider.services.firebaseAuthentication.FirebaseAuthenticationService
import com.raywenderlich.android.rwandroidtutorial.provider.services.resources.StringResourcesProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {
    @Inject lateinit var _firebaseAuthenticationService: FirebaseAuthenticationService
    @Inject lateinit var _stringResourcesProvider: StringResourcesProvider
    private lateinit var  binding: ActivityAuthBinding
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var auth:  FirebaseAuth
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var oneTapClient: SignInClient

    lateinit var txtEmail:      EditText
    lateinit var txtPass:       EditText
    lateinit var authLayout:    LinearLayout

    private val REQUEST_ONE_TAP = 2 // Puede ser cualquier entero unico para el Activity


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Setup
        this.setup()
        this.session()
        this.initialiceGoogleutentication()
    }

    override fun onStart() { // Se invoca cada vez que se vuelva a mostrar la pantalla
        super.onStart()
        // Mostramos de nuevo el layout en caso de que hagamos un log out y regresemos a este activity (pantalla)
        authLayout.visibility = View.VISIBLE // Hacemos visible el layout
    }

    /** Comprobacion de si existe una sesion activa **/
    private fun session() {
        val prefs: SharedPreferences = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email: String?      = prefs.getString("email", null)
        val provider: String?   = prefs.getString("provider", null)

        if ( email != null && provider != null ) {
            authLayout.visibility = View.INVISIBLE // Hacemos invisible el layout
            // TODO: Mostar este activity solo cuando hay datos sin registrar
            this.showRegistrationForm(email ?: "", ProviderType.valueOf(provider ?: ""))
        }
    }

    // TODO: Refactorizar metodo
    private fun setup() {
        txtEmail    = this.binding.txtEmail
        txtPass     = this.binding.txtPassword
        authLayout  = this.binding.authLayout

        title = "Authenticacion" // Modificamos el titulo de la pantalla

        auth = Firebase.auth

        this.binding.btnSignUp.setOnClickListener {
            this.authViewModel.signUp(
                this.binding.txtEmail.text.toString(), this.binding.txtPassword.text.toString()
            )
        }

        this.binding.btnLogin.setOnClickListener {
//            this.authViewModel.signIn(
//                this.binding.txtEmail.text.toString(), this.binding.txtPassword.text.toString()
//            )
        }

        this.binding.btnGoogleSignIn.setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener {result ->
                    try {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender, REQUEST_ONE_TAP, null, 0, 0, 0, null
                        )
                    }
                    catch (e: IntentSender.SendIntentException) {
                        Log.e("FirebaseAuth", "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener {
                    Log.d("FirebaseAuth", "${it.localizedMessage}")
                }
        }
    }

    /** Muestra una laerta de error **/
    private fun showAlert(message: String) {
        var builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage( message )
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    /** Muestra la pantalla par terminar el registro (RegistrationCompletionActivity) **/
    private fun showRegistrationForm( email: String, provider: ProviderType ) {
        val registrationIntent = Intent(this, RegistrationCompletionActivity::class.java).apply{
            // Paso de parametros a la nueva pantalla que se mostrara
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(registrationIntent)
    }

    /** Este metodo es llamada despues de que se selecciona la cuenta con la que se
     * iniciara sesion con oneTapClient. Es la el metodo donde se valida la respueta
     * del oneTapClient **/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { // TODO: Consultar documentacion sober onActivityResult
        super.onActivityResult(requestCode, resultCode, data)
        this.authViewModel.onActivityResultActions(requestCode, resultCode, data, oneTapClient)
    }

    /** Crea el registro correspondiente en la colecion "users" de Firestore **/
    private fun createUser( email: String, provider: ProviderType ) {
        val db = Firebase.firestore // Referencia a la DB Cloud Firestore definida en Firebase
        db.collection("users").document(email)
            .set(
                User(
                    cu = "",
                    career = "",
                    completeInformation = false,
                    provider = provider.name,
                    enable = true,
                    semester = 1
                )
            )
            .addOnSuccessListener {
                Log.d("Registro exitoso", "Datos del usuario agregados correctamente")
            }
            .addOnFailureListener {
                Log.w("Registro fallido", "No se ha logrado realizar el registro de los datos")
            }
    }

    /** Inicializacion de propiedades para usar el oneTapClient de inicio de sesion
     * com Google **/
    private fun initialiceGoogleutentication() {
        oneTapClient = Identity.getSignInClient(this)

        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(
                        _stringResourcesProvider.getString(R.string.default_web_client_id)
                    )
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()
    }
}
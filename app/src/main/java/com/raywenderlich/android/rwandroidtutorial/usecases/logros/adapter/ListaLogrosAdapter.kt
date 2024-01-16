package com.raywenderlich.android.rwandroidtutorial.usecases.logros.adapter


import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.raywenderlich.android.runtracking.R
import com.raywenderlich.android.rwandroidtutorial.models.Logro
import com.raywenderlich.android.rwandroidtutorial.provider.BDsqlite
import com.raywenderlich.android.rwandroidtutorial.provider.DatosUsuario


class ListaLogrosAdapter(private val logrosList:ArrayList<Logro>, private val context : Context) : RecyclerView.Adapter<ListaLogrosAdapter.ListaLogrosViewHolder>() {

    var pasosT = obtenerpasos()

    private fun obtenerpasos(): Int {
        // Obtener nombre de usuario
        val userName = DatosUsuario.getUserName(context) ?: "NombrePorDefecto"
        //Obtener datos de sqlite
        val db = BDsqlite(context)
        // Obtener datos para el usuario
        val cursorPasosTotales = db.getData(BDsqlite.getColumnPasosTotales(), userName)
        cursorPasosTotales.moveToFirst()
        val pasosTotales = cursorPasosTotales.getInt(0)
        return pasosTotales
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListaLogrosViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listalogros,parent,false)
        return ListaLogrosViewHolder(view)
    }


    override fun onBindViewHolder(holder: ListaLogrosViewHolder, position: Int) {

        val currentitem = logrosList[position]
        holder.titulo.text = currentitem.titulo
        holder.descripcion.text = currentitem.descripcion
        var pasos = currentitem.pasos.toString()
        if (Integer.parseInt(pasos)<=pasosT){
            holder.progreso.text = "Finalizado"
            holder.progreso.setTextColor(Color.parseColor("#FF0049"))
            holder.pasos.text = ""+pasos+" pasos de "+pasos+""
        }else{
            holder.progreso.text = "En curso"
            holder.pasos.text = ""+pasosT+" pasos de "+pasos+""
        }
        Glide.with(holder.photo.context).load(currentitem.photo).into(holder.photo)
    }

    override fun getItemCount(): Int {
        return logrosList.size
    }

    class ListaLogrosViewHolder(view: View) : RecyclerView.ViewHolder(view){

        val titulo:TextView = view.findViewById(R.id.logroname)
        val descripcion:TextView = view.findViewById(R.id.logrodescripcion)
        val pasos:TextView = view.findViewById(R.id.logroprogreso)
        val progreso:TextView = view.findViewById(R.id.logrostatus)
        val photo:ImageView = view.findViewById(R.id.imlogro)
    }

}
package com.example.mybluetoothapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_auth.*


class AuthActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        setup()
        session()

    }

    private fun setup() {
        title = "Identificate"

        //Metodo para crear usuario
        signUpButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty() && nameEditText.text.isNotEmpty() && cityEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                    emailEditText.text.toString(),
                    passwordEditText.text.toString(),
                ).addOnCompleteListener {
                    if (it.isSuccessful) {
                        saveUsers(it.result?.user?.email ?: "",nameEditText.text.toString(),cityEditText.text.toString())
                        showHome(it.result?.user?.email ?: "", nameEditText.text.toString(),cityEditText.text.toString())
                    } else {
                        showAlert()
                    }
                }
            }
        }

        //Metodo para acceder
        loginButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    emailEditText.text.toString(),
                    passwordEditText.text.toString()
                ).addOnCompleteListener {
                    if (it.isSuccessful) {
                        getUserData(it.result?.user?.email ?: "")
                    } else {
                        showAlert()
                    }
                }
            }
        }
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autentificado al usuario")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }



    private fun showHome(email: String, name: String, city: String) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("name", name)
            putExtra("city", city)
        }
        startActivity(homeIntent)
    }

    private fun session(){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val name = prefs.getString("name", null)
        val city = prefs.getString("city", null)

        if(email != null && name != null && city != null){
            authLayout.visibility = View.INVISIBLE
            //Toast.makeText(this, " sesion $email $name $city", Toast.LENGTH_SHORT).show()
            showHome(email, name, city)
        }
    }

    override fun onStart(){
        super.onStart()
        authLayout.visibility = View.VISIBLE
    }

    //Guadar datos en firebase
    private fun saveUsers(email: String, name: String, city: String){
        db.collection("users").document(email).set(
            hashMapOf("email" to email, "name" to name, "city" to city)
        )
    }

    private fun getUserData(email: String){
        db.collection("users").document(email).get().addOnSuccessListener {
            showHome(email,it.get("name") as String, it.get("city") as String)
        }
    }




}
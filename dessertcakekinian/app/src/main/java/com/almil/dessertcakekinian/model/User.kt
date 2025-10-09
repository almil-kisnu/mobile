package com.almil.dessertcakekinian.model

data class User(
    val username: String,
    val password: String,
    val role: String // "admin" atau "user"
)
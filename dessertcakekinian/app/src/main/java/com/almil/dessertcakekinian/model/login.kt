package com.almil.dessertcakekinian.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class User(
    @SerialName("iduser")
    val idUser: Int,

    @SerialName("username")
    val username: String,

    @SerialName("password")
    val password: String,

    @SerialName("role")
    val role: String,

    @SerialName("phone")
    val phone: String,

    @SerialName("createdat")
    val createdAt: String,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("deactivated_at")
    val deactivatedAt: String? = null,

    @SerialName("deactivated_reason")
    val deactivatedReason: String? = null,

    @SerialName("hired_date")
    val hiredDate: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("idoutlet")
    val idOutlet: Int? = null,

    @SerialName("nik")
    val nik: String? = null
)

@Serializable
data class Outlet(
    @SerialName("idoutlet")
    val idOutlet: Int,

    @SerialName("kode_outlet")
    val kodeOutlet: String,

    @SerialName("nama_outlet")
    val namaOutlet: String,

    @SerialName("alamat")
    val alamat: String? = null,

    @SerialName("telepon")
    val telepon: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    val createdAt: String? = null
)
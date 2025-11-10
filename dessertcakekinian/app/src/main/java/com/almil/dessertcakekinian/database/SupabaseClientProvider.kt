package com.almil.dessertcakekinian.database

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    private const val SUPABASE_URL = "https://rujrwhtwkoferxhhnruq.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ1anJ3aHR3a29mZXJ4aGhucnVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkxNDYwODYsImV4cCI6MjA3NDcyMjA4Nn0.v7ALlCUbNBDy2TWQ0rhGc0tR0qgnR4J9ko8v-8Bwrww"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth)   // auth
            install(Postgrest) // query database
            install(Storage) //kalau butuh upload file
            install(Realtime) //kalau butuh live data
        }
    }
}
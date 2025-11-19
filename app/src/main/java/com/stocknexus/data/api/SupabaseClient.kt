package com.stocknexus.data.api

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://gvlaokxdgcnttyovdhku.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd2bGFva3hkZ2NudHR5b3ZkaGt1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkxMDMwNzEsImV4cCI6MjA3NDY3OTA3MX0.yAHnpoADPvk0rMnyoWiloKFxJJAPbkbKi9KtgwGdWCw"
    ) {
        install(Auth)
        install(Postgrest)
    }
}

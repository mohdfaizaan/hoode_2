package com.example.hoode_app.data.remote

object SupabaseConfig {
    // Configurable endpoints for production Supabase backend
    var supabaseUrl: String = "https://example-project.supabase.co"
    var supabaseKey: String = "public-anon-key-placeholder"

    val isConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") && !supabaseUrl.contains("example-project")
}

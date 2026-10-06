package com.example.hoode_app.ui.profile
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.hoode_app.R
class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container=FrameLayout(this).apply { id=R.id.profile_host }
        setContentView(container)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(container){view,insets->
            val bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);insets
        }
        if(savedInstanceState==null) {
            val host=NavHostFragment()
            supportFragmentManager.beginTransaction().replace(container.id,host).setPrimaryNavigationFragment(host).commitNow()
            val graph=host.navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(R.id.profileFragment)
            host.navController.graph=graph
        }
    }
}

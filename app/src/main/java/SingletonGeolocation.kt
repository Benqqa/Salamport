import android.app.Activity
import android.content.Context
import android.os.Handler
import com.location.aravind.getlocation.GeoLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await

object SingletonGeolocation {

    private val h = Handler()

    fun init(app: Context, activity: Activity, session: String, token: String) {
        val client = OkHttpClient.Builder().build()
        val th = Runnable {
            if (activity != null) {
                sendData(app, activity, session, token, client)
            }
        }
        h.postDelayed(th, 600000)
    }


    private fun sendData(
        app: Context, activity: Activity, session: String, token: String,
        client: OkHttpClient
    ) {
        if (activity != null) {
            val geoLocator = GeoLocator(app, activity)
            val a = geoLocator.lattitude
            val b = geoLocator.longitude

            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("token", token)
                    .add("session", session)
                    .add("lat", a.toString())
                    .add("lon", b.toString())
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/send_geo.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseString =
                    withContext(Dispatchers.IO) {
                        client.newCall(request).await().body?.string()
                    }
            }
            h.postDelayed({ sendData(app, activity, session, token, client) }, 5000)
        }
    }
}

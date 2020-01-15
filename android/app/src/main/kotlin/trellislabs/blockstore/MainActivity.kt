package trellislabs.blockstore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import org.blockstack.android.sdk.*
import org.blockstack.android.sdk.model.BlockstackConfig
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.blockstack.android.sdk.model.UserData
import java.util.*
import kotlin.Result

class MainActivity: FlutterActivity() {
    private var _blockstackSession: BlockstackSession? = null
    private val CHANNEL = "BlockStore"
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    GeneratedPluginRegistrant.registerWith(this)
      val config = BlockstackConfig(
              URI("https://flamboyant-darwin-d11c17.netlify.com"),
              "/redirect",
              "/manifest.json",
              arrayOf(BaseScope.StoreWrite.scope))
      val sessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(this))
      val blockstack = Blockstack()
      _blockstackSession = BlockstackSession(sessionStore, config, blockstack = blockstack)
      GlobalScope.launch (Dispatchers.IO){Log.e("checkout", BlockstackSession(sessionStore, config, blockstack = blockstack).isUserSignedIn().toString())  }

      checkForSession()

    MethodChannel(flutterView, CHANNEL).setMethodCallHandler { call, result ->
        var ctex = this
      GlobalScope.launch(Dispatchers.IO){
          get(ctex)
      }
      result.success(true)

      //startService(privateKey!!, value!!)
    }

    

  }
    fun checkForSession() : Boolean {
        val session = _blockstackSession
        Log.e("SigninStatus",session!!.isUserSignedIn().toString())
        Log.e("SigninStatus",(session != null).toString())
        return session != null
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == Intent.ACTION_VIEW) {
            GlobalScope.launch(Dispatchers.IO){
                handleAuthResponse(intent, this@MainActivity)
            }

        }
    }

    fun blockstackSession() : BlockstackSession {
        val session = _blockstackSession
        if(session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }
    private suspend fun handleAuthResponse(intent: Intent, context:Context) {
        val response = intent.data?.query
        if (response != null) {
            val authResponseTokens = response.split('=')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.e("signin data", authResponse)
                withContext(Dispatchers.IO){
                    val userDataResult = blockstackSession().handleUnencryptedSignIn(authResponse)
                    if (userDataResult.hasValue) {
                        val userData = userDataResult.value!!
                        Log.e("On Sign In status", "signed in!" + userData.appPrivateKey)
                        //Log.e("Signin error", "error: ${userDataResult.error}")
                        val sessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(context))
                        sessionStore.updateUserData(userData)
                        checkForSession()
                        // Toast.makeText(this@MainActivity, "Sigined in", Toast.LENGTH_SHORT).show()

                    } else {

                        Log.e("Signin error", "error: ${userDataResult.error}")
                        // Toast.makeText(this@MainActivity, "error: ${userDataResult.error}", Toast.LENGTH_SHORT).show()

                    }
                }
            }
        }
    }


    suspend fun get(context: Context) =
          withContext(Dispatchers.IO) {

            val config = BlockstackConfig(
                    URI("https://flamboyant-darwin-d11c17.netlify.com"),
                    "/redirect",
                    "/manifest.json",
                    arrayOf(BaseScope.StoreWrite.scope))
              val sessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(context))
            val blockstackSignIn = BlockstackSignIn(sessionStore, config)
              val key = blockstackSignIn.generateAndStoreTransitKey()
            //var network = Network("https://core.blockstack.org")
              //val authRequest = blockstackSignIn.makeAuthRequest(key, Date(System.currentTimeMillis() + 3600000).time, mapOf(Pair("solicitGaiaHubUrl", true)))
              blockstackSignIn.redirectUserToSignIn(this@MainActivity)
          }
}

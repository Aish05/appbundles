package com.aish.appbundle

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity() {

    companion object {
        private const val CONFIRMATION_REQUEST_CODE = 1
        private const val PACKAGE_NAME = "com.aish.customer_support"
        private const val CUSTOMER_SUPPORT_CLASSNAME = "$PACKAGE_NAME.CustomerSupportActivity"
    }

    private lateinit var manager: SplitInstallManager

    //can use while installing multiple features at time
    private val installableModules by lazy { listOf("customer_support") }

    /** Listener used to handle changes in state for install requests. */
    private val listener = SplitInstallStateUpdatedListener { state ->
        val names = state.moduleNames().joinToString(" - ")

        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                //  In order to see this, the application has to be uploaded to the Play Store.
                displayLoadingState(state, getString(R.string.downloading, names))
            }
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                /*
                  This may occur when attempting to download a sufficiently large module.
                  In order to see this, the application has to be uploaded to the Play Store.
                  Then features can be requested until the confirmation path is triggered.
                 */
                manager.startConfirmationDialogForResult(state, this, CONFIRMATION_REQUEST_CODE)
            }
            SplitInstallSessionStatus.INSTALLED -> {
                onSuccessfulLoad(names, launch = true)
            }

            SplitInstallSessionStatus.INSTALLING -> displayLoadingState(
                state, getString(R.string.installing, names)
            )
            SplitInstallSessionStatus.FAILED -> {
                Log(
                    getString(
                        R.string.error_for_module, state.errorCode(),
                        state.moduleNames()
                    )
                )
            }
            SplitInstallSessionStatus.CANCELED -> {

            }
            SplitInstallSessionStatus.CANCELING -> {

            }
            SplitInstallSessionStatus.DOWNLOADED -> {

            }
            SplitInstallSessionStatus.PENDING -> {

            }
            SplitInstallSessionStatus.UNKNOWN -> {

            }
        }
    }

    private fun onSuccessfulLoad(moduleName: String, launch: Boolean) {
        if (launch) {
            //can use module name with switch when having multiple module names
            launchActivity(CUSTOMER_SUPPORT_CLASSNAME)
        }
    }

    private fun launchActivity(className: String) {
        val intent = Intent().setClassName(BuildConfig.APPLICATION_ID, className)
        startActivity(intent)
    }

    private fun displayLoadingState(state: SplitInstallSessionState?, message: String) {
        displayProgress()
        updateProgressMessage(message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        manager = SplitInstallManagerFactory.create(this)
        initViews()
    }

    private fun initViews() {
        load_customer_support.setOnClickListener {
            loadAndLaunchModule("customer_support")
        }
    }

    override fun onResume() {
        // Listener can be registered even without directly triggering a download.
        manager.registerListener(listener)
        super.onResume()
    }

    override fun onPause() {
        // Make sure to dispose of the listener once it's no longer needed.
        manager.unregisterListener(listener)
        super.onPause()
    }

    /** This is needed to handle the result of the manager.startConfirmationDialogForResult request */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CONFIRMATION_REQUEST_CODE) {
            // Handle the user's decision. For example, if the user selects "Cancel",
            // you may want to disable certain functionality that depends on the module.
            if (resultCode == Activity.RESULT_CANCELED) {
                Log(getString(R.string.user_cancelled))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /** request start install and on success load the  */
    private fun loadAndLaunchModule(moduleName: String) {
        updateProgressMessage(getString(R.string.loading_module, moduleName))
        // Skip loading if the module already is installed. Perform success action directly.
        if (manager.installedModules.contains(moduleName)) {
            updateProgressMessage(getString(R.string.already_installed))
            onSuccessfulLoad(moduleName, launch = true)
            return
        }

        // Create request to install a feature module by name.
        val request = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()

        // Load and install the requested feature module.
        manager.startInstall(request)
            .addOnSuccessListener {
                Log("main: addOnSuccessListener")
            }
            .addOnFailureListener {
                Log("main: addOnFailureListener")
            }
            .addOnCompleteListener {
                Log("main: addOnCompleteListener")
            }

        updateProgressMessage(getString(R.string.starting_install_for, moduleName))
    }

    /** Install all features but do not launch any of them. */
    private fun installAllFeaturesNow() {
        // Request all known modules to be downloaded in a single session.
        val requestBuilder = SplitInstallRequest.newBuilder()

        installableModules.forEach { name ->
            if (!manager.installedModules.contains(name)) {
                requestBuilder.addModule(name)
            }
        }

        val request = requestBuilder.build()

        manager.startInstall(request).addOnSuccessListener {
            Log("Loading ${request.moduleNames}")
        }.addOnFailureListener {
            Log("Failed loading ${request.moduleNames}")
        }
    }

    /** Uninstall a module. */
    private fun requestUninstall() {
        Log("Requesting uninstall of all modules." +
                    "This will happen at some point in the future.")

        val installedModules = manager.installedModules.toList()
        manager.deferredUninstall(installedModules).addOnSuccessListener {
            Log("Uninstalling $installedModules")
        }.addOnFailureListener {
            Log("Failed installation of $installedModules")
        }
    }

    private fun updateProgressMessage(message: String) {
        if (progress_bar.visibility != View.VISIBLE) displayProgress()
        progress_text.text = message
    }

    /** Display progress bar and text. */
    private fun displayProgress() {
        //TODO Make it visible later
        progress_bar.visibility = View.GONE
    }
}

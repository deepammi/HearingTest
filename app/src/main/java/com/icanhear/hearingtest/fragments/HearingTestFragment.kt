package com.hearingtest.hearingtest.fragments

//
//import kotlinx.android.synthetic.main.fragment_user_info.*
import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioTrack
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.hearingtest.hearingtest.*
import com.hearingtest.hearingtest.databinding.FragmentHearingTestBinding
import com.hearingtest.hearingtest.inapp.InAppBilling
import com.hearingtest.hearingtest.models.State
import com.hearingtest.hearingtest.models.UserInfo
import com.icanhear.hearingtest.FirebaseCallback
import com.icanhear.hearingtest.NetworkUtils.isNetworkAvailable
import java.io.File
import java.io.FileInputStream
import java.util.*

class HearingTestFragment : Fragment(), StateChangeListener, FirebaseCallback {


    // Change grap and updated dependency to configure latest android version update

    private lateinit var binding: FragmentHearingTestBinding

    private var soundGenerator: SoundGenerator? = null

    private var hearingTest: HearingTest? = null

    private var audioTrack: AudioTrack? = null

    private var timer = Timer()

    private var inAppBilling: InAppBilling? = null
    private var isResultSaved = false
    private var isPendingSaveAfterPermission = false


    private var timerTask = object : TimerTask() {
        override fun run() {
            SoundGen().execute()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            soundGenerator = SoundGenerator()
            soundGenerator?.let {
                hearingTest = HearingTest(this)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHearingTestBinding.inflate(inflater, container, false)

        with(binding) {
            yes.setOnClickListener { hearingTest?.yes() }
            no.setOnClickListener { hearingTest?.no() }
            save.setOnClickListener(this@HearingTestFragment::save)
            supportUs.setOnClickListener(this@HearingTestFragment::initiateBilling)
        }

        setup()
        start()
        inAppBilling = InAppBilling()
        inAppBilling!!.setupBillingClient(requireContext())

        return binding.root
    }
    override fun onResume() {
        super.onResume()
        val context = context ?: return

        val isManagerPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (isManagerPermissionGranted && isPendingSaveAfterPermission && !isResultSaved) {
            save(binding.save)
            isPendingSaveAfterPermission = false
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isPendingSaveAfterPermission && !isResultSaved) {
                    save(binding.save)
                    isPendingSaveAfterPermission = false
                }
            } else {
                Snackbar.make(
                    binding.save,
                    "Permission has not been granted",
                    Snackbar.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Permission has not been granted")
            }
        }
    }


    override fun onDestroyView() {
        stop()
        super.onDestroyView()
    }

    private fun setup() {
        hearingTest?.start()
        binding.pulsator.start()
        initChart()
        initUserInfo()
    }

    private fun initChart() {

        with(binding) {

            chart.setBackgroundColor(Color.WHITE)
            chart.description.isEnabled = false
            chart.setTouchEnabled(false)
            chart.setDrawGridBackground(false)
            chart.isDragEnabled = false
            chart.setScaleEnabled(false)
            chart.setPinchZoom(false)

            val xAxis = chart.xAxis
            xAxis.axisMaximum = 9000f
            xAxis.axisMinimum = 0f
            xAxis.setDrawLimitLinesBehindData(true)
            xAxis.gridColor = Color.LTGRAY

            chart.axisRight.isEnabled = true

            val yAxis = chart.axisLeft
            yAxis.axisMaximum = 120f
            yAxis.axisMinimum = 0f
            yAxis.setDrawLimitLinesBehindData(true)
            yAxis.gridColor = Color.LTGRAY

            chart.legend.form = Legend.LegendForm.NONE

            chart.axisLeft.isInverted = true
            chart.axisRight.isInverted = false


            setData()
        }
    }

    private fun setData() {
        hearingTest?.let { hearingTest ->

            val rightEarValues =
                ArrayList(hearingTest.rightEarResults.map { Entry(it.frequency, it.dB) })
            val leftEarValues =
                ArrayList(hearingTest.leftEarResults.map { Entry(it.frequency, it.dB) })

            with(binding) {

                if (chart.data != null && chart.data.dataSetCount > 0) {

                    val rightSet = chart.data.getDataSetByIndex(0) as LineDataSet
                    rightSet.values = rightEarValues
                    rightSet.notifyDataSetChanged()

                    val leftSet = chart.data.getDataSetByIndex(1) as LineDataSet
                    leftSet.values = leftEarValues
                    leftSet.notifyDataSetChanged()

                    chart.data.notifyDataChanged()
                    chart.notifyDataSetChanged()
                } else {
                    val rightSet = generateSet(rightEarValues, chart, R.color.right_ear_color)
                    val leftSet = generateSet(leftEarValues, chart, R.color.left_ear_color)

                    val dataSets = ArrayList<ILineDataSet>()
                    dataSets.add(rightSet)
                    dataSets.add(leftSet)

                    val data = LineData(dataSets)
                    chart.data = data
                }

                chart.animateX(0)

                chart.invalidate()
            }
        }
    }

    private fun generateSet(
        entryList: ArrayList<Entry>,
        chart: LineChart,
        colorRes: Int
    ): LineDataSet {
        val set = LineDataSet(entryList, "")
        set.setDrawIcons(false)

        context?.apply {
            set.color = ContextCompat.getColor(this, colorRes)
            set.setCircleColor(ContextCompat.getColor(this, colorRes))
            set.lineWidth = 4f
            set.circleRadius = 6f
            set.circleHoleRadius = 5f
            set.setDrawCircleHole(true)
            set.formLineWidth = 0f
            set.formSize = 15f
            set.valueTextSize = 10f
            set.setDrawFilled(true)
            set.fillFormatter =
                IFillFormatter { _, _ -> chart.axisLeft.axisMinimum }
            set.fillColor = ContextCompat.getColor(this, colorRes)
            set.fillAlpha = 10
        }

        return set
    }


    private fun start() {
        hearingTest?.start()
        timer.schedule(timerTask, 0, 1000)
    }

    private fun stop() {
        timer.cancel()
        timerTask.cancel()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class SoundGen : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg hz: Unit?) {
            audioTrack?.let {
                audioTrack?.release()
                audioTrack = null
            }
            hearingTest?.currentState?.let { state ->
                val userInfo = UserInfo.instance
                if (!TextUtils.isEmpty(userInfo.gainFactor)) {
                    val gainFactro: Int = userInfo.gainFactor.toInt()
                    audioTrack = soundGenerator?.playSoundWithGainFactor(
                        state.frequency,
                        state.dB,
                        state.channel,
                        gainFactro
                    )
                } else {
                    audioTrack = soundGenerator?.playSoundWithoutGainFactor(
                        state.frequency,
                        state.dB,
                        state.channel
                    )
                }
                audioTrack?.play()
            } ?: run {
                audioTrack = null
            }
        }
    }

    private fun initiateBilling(view: View) {
        inAppBilling!!.loadBilling(requireActivity())

    }



    private fun save(view: View) {
        binding.progressBarSaving.visibility = View.VISIBLE
        binding.supportUs.isEnabled = false
        if (isResultSaved) {
            Snackbar.make(view, "This result has already been saved.", Snackbar.LENGTH_SHORT).show()
            return
        }

        context?.let { context ->
            val fileName = hearingTest?.saveResult(context, this)
            if (fileName.isNullOrEmpty()) {
                Snackbar.make(view, "Error: Unable to save file.", Snackbar.LENGTH_SHORT).show()
                return@let
            }

            // Upload the saved file to Firebase Storage if internet available
            if (!isNetworkAvailable(context)) {
                binding.progressBarSaving.visibility = View.GONE
                binding.supportUs.isEnabled = true
                Snackbar.make(view, "No internet connection. Results will be saved locally only.", Snackbar.LENGTH_LONG).show()
                return@let
            }

            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val fileRef = storageRef.child(fileName)
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HearingTest/$fileName")
            val stream = FileInputStream(file)

           /* val uploadTask = fileRef.putStream(stream)
            uploadTask.addOnFailureListener {
                binding.progressBarSaving.visibility = View.GONE
                binding.supportUs.isEnabled = true
                Snackbar.make(view, "Upload failed: ${it.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }.addOnSuccessListener {
                binding.progressBarSaving.visibility = View.GONE
                binding.supportUs.isEnabled = true
                Snackbar.make(view, "File uploaded successfully: $fileName", Snackbar.LENGTH_SHORT).show()
                isResultSaved = true
            } */
        }
    }



//    private fun supportUs(view: View) {
//        context?.let {
//
//
//            val payPalPayment = PayPalPayment(
//                BigDecimal("20".toString()), "USD",
//                "Purchase Goods", PayPalPayment.PAYMENT_INTENT_SALE
//            )
//            val intent = Intent(context, PaymentActivity::class.java)
//            intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config)
//            intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payPalPayment)
//            startActivityForResult(intent, PAYPAL_REQUEST_CODE)
//
//
//        }
//    }


    override fun onChanged(
        state: State,
        progressMax: Int,
        progressCurrent: Int,
        approxStop: Float,
        confirmation: Int
    ) {
        val frequency = "${state.frequency} Hz"
        val pressure = "${state.dB} dB"
        val question = context?.getString(R.string.do_you_hear_the_tone)
        val questionWithProgress = "$question ($progressCurrent/$progressMax)"
        binding.frequency.text = frequency
        binding.pressure.text = pressure
        binding.question.text = questionWithProgress

        setData()
    }

    override fun onFinish() {
        binding.soundTestLayout.visibility = View.GONE
//        binding.save.visibility = View.VISIBLE
        binding.chart.visibility = View.VISIBLE
        binding.labelYaxis.visibility = View.VISIBLE
        binding.resultContainer.visibility = View.VISIBLE
        binding.support.visibility = View.VISIBLE
        if (!isResultSaved) {
            save(binding.save)
        }
        stop()
    }

    override fun onSuccess(message: String) {
        binding.progressBarSaving.visibility = View.GONE
        isResultSaved = true
        binding.supportUs.isEnabled = true
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onFailure(error: String) {
        binding.progressBarSaving.visibility = View.GONE
        binding.supportUs.isEnabled = true
        isResultSaved = true
        activity?.runOnUiThread {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            Log.e("Firestore", error)
        }
    }

    private fun initUserInfo() {
        val userInfo = UserInfo.instance
        with(binding) {
            name.text = userInfo.name
            location.text = userInfo.location
            age.text = userInfo.age
            email.text = userInfo.email
            phone.text = userInfo.phone
            history.text = userInfo.medicalHistory
            gainFactor.text = userInfo.gainFactor

            nameContainer.visibility =
                if (TextUtils.isEmpty(userInfo.name)) View.GONE else View.VISIBLE
            locationContainer.visibility =
                if (TextUtils.isEmpty(userInfo.location)) View.GONE else View.VISIBLE
            ageContainer.visibility =
                if (TextUtils.isEmpty(userInfo.age)) View.GONE else View.VISIBLE
            emailContainer.visibility =
                if (TextUtils.isEmpty(userInfo.email)) View.GONE else View.VISIBLE
            phoneContainer.visibility =
                if (TextUtils.isEmpty(userInfo.phone)) View.GONE else View.VISIBLE
            historyContainer.visibility =
                if (TextUtils.isEmpty(userInfo.medicalHistory)) View.GONE else View.VISIBLE

            gainFactorContainer.visibility =
                if (TextUtils.isEmpty(userInfo.gainFactor)) View.GONE else View.VISIBLE
        }
    }


//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == PAYPAL_REQUEST_CODE) {
//            if (resultCode == Activity.RESULT_OK) {
//                val confirmation: PaymentConfirmation =
//                    data!!.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION)
//                if (confirmation != null) {
//                    try {
//
////                        val paymentDetails = confirmation.toJSONObject().toString(4)
////                        startActivity(
////                            Intent(context, PaymentDetails::class.java)
////                                .putExtra("PaymentDetails", paymentDetails)
////                                .putExtra("PaymentAmount", "20")
////                        )
//                        showMessage("Your payment has been received !")
//                    } catch (e: JSONException) {
//                        e.printStackTrace()
//                    }
//                }
//            } else if (resultCode == Activity.RESULT_CANCELED) Toast.makeText(
//                context,
//                "Cancel",
//                Toast.LENGTH_SHORT
//            ).show()
//        } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) Toast.makeText(
//            context,
//            "Invalid",
//            Toast.LENGTH_SHORT
//        ).show()
//    }


    private fun showMessage(message: String) {
        val dialogBuilder = context?.let { AlertDialog.Builder(it) }

        // set message of alert dialog
        dialogBuilder!!.setMessage("Thanks for supporting us with 20$ $message")
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action

            // negative button text and action
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                dialog.cancel()
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle("Support Us ")
        // show alert dialog
        alert.show()

    }

    companion object {
        private const val TAG = "HEARING_TEST_FRAGMENT"
        private const val STORAGE_PERMISSION_REQUEST = 112
        private const val PAYPAL_REQUEST_CODE = 114
    }
}
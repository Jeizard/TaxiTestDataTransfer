package com.jeizard.taxi

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jeizard.taxi.databinding.ActivityTaxiBinding


class TaxiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaxiBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaxiBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val intent: Intent = intent
        binding.telNumberTextView.text = intent.getStringExtra("Telephone number")
        binding.nameTextView.text = intent.getStringExtra("Name") + " " + intent.getStringExtra("Surname")

        binding.setPathButton.setOnClickListener(View.OnClickListener {
            val intent: Intent = Intent(this, PathActivity::class.java)
            // startActivityForResult(intent, 1) // deprecated

            resultLauncher.launch(intent)
        })

        binding.callTaxiButton.setOnClickListener(View.OnClickListener {
            Toast.makeText(this, "Wait for Taxi. Good luck!", Toast.LENGTH_LONG).show()
        })
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if(data != null){
                binding.pathTextView.text = "Taxi will arrive at " + data.getStringExtra("Point From") + " in " + data.getStringExtra("Time") + " and take you in " +  data.getStringExtra("Point To") + ". If you are agree click Call Taxi."

                binding.callTaxiButton.isEnabled = true
                binding.callTaxiButton.setBackgroundColor(Color.BLACK)
            }
        }
    }
}

//        binding.okButton.setOnClickListener(View.OnClickListener {
//            val intent: Intent = Intent(this, TaxiActivity::class.java)
//            val pointFrom: String = binding.streetFromEditText.text.toString() + ", " + binding.houseFromEditText.text.toString() + ", " + binding.flatFromEditText.text.toString()
//            intent.putExtra("Point From", pointFrom)
//            val pointTo: String = binding.streetToEditText.text.toString() + ", " + binding.houseToEditText.text.toString() + ", " + binding.flatToEditText.text.toString()
//            intent.putExtra("Point To", pointTo)
//
//            setResult(RESULT_OK, intent);
//            finish();
//        })
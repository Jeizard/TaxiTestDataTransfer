package com.jeizard.taxi

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.Selection
import android.text.Spanned
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jeizard.taxi.databinding.ActivityAuthBinding
import java.util.Locale

const val PREFS_FILENAME = "USER INFO"


class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var sharedPrefs: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPrefs = this.getSharedPreferences(PREFS_FILENAME, MODE_PRIVATE)
        val telNumber = sharedPrefs.getString("Telephone number", "+375")
        val name = sharedPrefs.getString("Name", "")
        val surname = sharedPrefs.getString("Surname", "")
        if(!(telNumber.equals("+375") || name.toString().isEmpty() || surname.toString().isEmpty())) {
            binding.authButton.text = "LOG IN"
        }
        binding.telNumberEditText.addTextChangedListener(PhoneNumberFormattingTextWatcher())

        binding.telNumberEditText.setText(telNumber)
        binding.nameEditText.setText(name)
        binding.surnameEditText.setText(surname)

        binding.authButton.isEnabled = binding.telNumberEditText.text.length == 17 &&
                binding.nameEditText.text.length > 2 &&
                binding.surnameEditText.text.length > 2

        var authChangedListener: TextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(binding.telNumberEditText.text.toString() == telNumber &&
                    binding.nameEditText.text.toString() == name &&
                    binding.surnameEditText.text.toString() == surname){
                    binding.authButton.text = "LOG IN"
                }
                else{
                    binding.authButton.text = "REGISTRATION"
                }

                binding.authButton.isEnabled = binding.telNumberEditText.text.length == 17 &&
                        binding.nameEditText.text.length > 2 &&
                        binding.surnameEditText.text.length > 2
            }
        }

        binding.nameEditText.filters = arrayOf(CustomInputFilter())
        binding.surnameEditText.filters = arrayOf(CustomInputFilter())

        binding.telNumberEditText.addTextChangedListener(authChangedListener)
        binding.nameEditText.addTextChangedListener(authChangedListener)
        binding.surnameEditText.addTextChangedListener(authChangedListener)

        binding.authButton.setOnClickListener(View.OnClickListener {
            val intent: Intent = Intent(this, TaxiActivity::class.java)
            intent.putExtra("Telephone number", binding.telNumberEditText.text.toString())
            intent.putExtra("Name", binding.nameEditText.text.toString())
            intent.putExtra("Surname", binding.surnameEditText.text.toString())
            startActivity(intent)
        })

        binding.telNumberEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(!s.toString().startsWith("+375")) {
                    binding.telNumberEditText.setText("+375")
                    binding.telNumberEditText.setSelection(binding.telNumberEditText.text.length)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text: String = binding.telNumberEditText.text.toString()
                val textLength = text.length

                if(textLength == 5 || textLength == 8 || textLength == 12 || textLength == 15) {
                    binding.telNumberEditText.setText(StringBuilder(text).insert(textLength - 1, " ").toString())
                    binding.telNumberEditText.setSelection(binding.telNumberEditText.text.length)
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()

        val editor = sharedPrefs.edit()
        editor.putString("Telephone number", binding.telNumberEditText.text.toString())
        editor.putString("Name", binding.nameEditText.text.toString())
        editor.putString("Surname", binding.surnameEditText.text.toString())
        editor.apply()
    }

    class CustomInputFilter : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val regex = Regex("[a-zA-Zа-яА-Я]*")
            val input = source?.subSequence(start, end) ?: ""
            if (!input.matches(regex)) {
                return ""
            }
            val newText = dest?.subSequence(0, dstart).toString() + input +
                    dest?.subSequence(dend, dest.length).toString()
            if (newText.length > 10) {
                return newText.substring(0, 10)
            }
            return null
        }
    }
}
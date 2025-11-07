package com.example.laboratorytask1

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageSwitcher
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
//import android.widget.ViewSwitcher
//import androidx.core.content.ContextCompat
//import androidx.wear.compose.material.Button

class MainActivity : AppCompatActivity() {

    private val images = arrayOf(
        R.drawable.beacver,
        R.drawable.penguin
    )

    private var currentIndex = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // IMAGE SWITCHER
        val imageSwitcher = findViewById<ImageSwitcher>(R.id.imageSwitcher)
        val nextBtn = findViewById<Button>(R.id.nextBtn)
        val prevBtn = findViewById<Button>(R.id.prevBtn)

        imageSwitcher.setFactory {
            val imageView = ImageView(applicationContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            imageView
        }

        // SETTING INITIAL IMAGE
        imageSwitcher.setImageResource(images[currentIndex])

        // ANIMATION
        imageSwitcher.inAnimation = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        imageSwitcher.outAnimation = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)

        // BUTTON CLICKS FOR IMAGE SWITCHING
        nextBtn.setOnClickListener {
            currentIndex = (currentIndex + 1) % images.size
            imageSwitcher.setImageResource(images[currentIndex])
        }

        prevBtn.setOnClickListener {
            currentIndex = (currentIndex - 1 + images.size) % images.size
            imageSwitcher.setImageResource(images[currentIndex])
        }
            
        // LOGIN BUTTON
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        loginBtn.setOnClickListener {
            Toast.makeText(this, "Button got clicked!", Toast.LENGTH_SHORT).show()
        }

        // PASSWORD TOGGLE BUTTON
        val seePassBtn = findViewById<ImageButton>(R.id.seePassBtn)
            seePassBtn.setOnClickListener {
                Toast.makeText(this, "Button got clicked!", Toast.LENGTH_SHORT).show()
            }

        // GEAR TOGGLE BUTTON
        val gearToggleBtn = findViewById< ToggleButton>(R.id.gearToggleBtn)
            gearToggleBtn.setOnClickListener {
                Toast.makeText(this, "Button got clicked!", Toast.LENGTH_SHORT).show()
            }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
            val acTxtView = findViewById<AutoCompleteTextView>(R.id.acTxtView)
            val fruits = arrayOf("Banana", "Strawberry", "Apple", "Blueberry", "Elderberry")
            val acTxtViewAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fruits)
            acTxtView.setAdapter(acTxtViewAdapter)

            val spinner = findViewById<Spinner>(R.id.spinnerOptions)
            val options = resources.getStringArray(R.array.simple_options)

            val spinnerAdapter = object: ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                options
            ){
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    val textView = view as TextView
                    textView.setTextColor(ContextCompat.getColor(context, R.color.spinnerItemColor))
                    return view
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    val textView = view as TextView
                    textView.setTextColor(ContextCompat.getColor(context, R.color.spinnerItemColor))
                    return view
                }
            }
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = spinnerAdapter

                spinner.setSelection(0)

    }


}
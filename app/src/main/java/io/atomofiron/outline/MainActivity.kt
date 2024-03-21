package io.atomofiron.outline

import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import io.atomofiron.outline.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val Int.dp: Float get() = this * resources.displayMetrics.density

    private var clipCanvasWasChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.config()
    }

    private fun ActivityMainBinding.config() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { root, windowInsets ->
            val insets = windowInsets.getInsets(Type.systemBars() or Type.displayCutout())
            root.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }
        val currentNight = resources.getBoolean(R.bool.night)
        theme.setImageResource(if (currentNight) R.drawable.ic_mode_day else R.drawable.ic_mode_night)
        theme.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(if (currentNight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES)
            recreate()
        }
        android.text = getString(R.string.android_version, SDK_INT)
        legacy.isChecked = legacyMode
        legacy.isEnabled = !isCurveUnavailable
        clipCanvas.isChecked = isCurveUnavailable
        clipCanvas.isEnabled = !isCurveWorks
        update()
        legacy.setOnCheckedChangeListener { _, _ -> update() }
        clipCanvas.setOnCheckedChangeListener { _, _ -> update() }
    }

    private fun ActivityMainBinding.update() {
        forceLegacy = legacy.isChecked

        val strokeRadius = 32.dp
        val strokeWidth = 2.dp
        val space = 2.dp
        val borderRadius = strokeRadius - strokeWidth - space
        val borderWidth = 1.dp

        val color = when (resources.getBoolean(R.bool.day)) {
            true -> Color.parseColor("#ff008800")
            false -> Color.parseColor("#ff00aa00")
        }
        val stroke = RoundCornersDrawable.stroke(color, strokeRadius, strokeWidth)
        frame.clipToOutline = true
        //frame.background = stroke
        frame.foreground = stroke
        frame.outlineProvider = stroke.getOutlineProvider()

        if (isCurveWorks) {
        } else if (legacy.isChecked && clipCanvas.isEnabled) {
            clipCanvas.isEnabled = false
            clipCanvasWasChecked = clipCanvas.isChecked
            clipCanvas.isChecked = false
        } else if (!legacy.isChecked && !clipCanvas.isEnabled) {
            clipCanvas.isEnabled = true
            clipCanvas.isChecked = clipCanvasWasChecked
        }

        image.setCornerRadius(if (clipCanvas.isChecked) borderRadius else 0f)
        image.setRoundedBorder(Color.parseColor("#55000000"), borderRadius, borderWidth)
    }
}
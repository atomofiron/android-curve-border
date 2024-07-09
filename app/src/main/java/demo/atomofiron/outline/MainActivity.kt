package demo.atomofiron.outline

import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import demo.atomofiron.outline.databinding.ActivityMainBinding

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
        legacy.isChecked = CurveDelegate.legacyMode
        legacy.isEnabled = !isCurveUnavailable
        clipCanvas.isChecked = !isCurvedOutlineWork
        clipCanvas.isEnabled = !isCurvedOutlineWork
        update()
        frame.setOnClickListener { }
        legacy.setOnCheckedChangeListener { _, _ -> update() }
        clipCanvas.setOnCheckedChangeListener { _, _ -> update() }
    }

    private fun ActivityMainBinding.update() {
        CurveDelegate.forceLegacy = legacy.isChecked

        val strokeRadius = 32.dp
        val strokeWidth = 2.dp
        val space = 2.dp
        val borderRadius = strokeRadius - strokeWidth - space
        val borderWidth = 1.dp

        val defaultColor = when (resources.getBoolean(R.bool.day)) {
            true -> Color.parseColor("#ff008800")
            false -> Color.parseColor("#ff00aa00")
        }.let { ColorType.Value(it) }
        val pressedColor = ColorType.Value(Color.MAGENTA, StateType(pressed = true))
        val stroke = RoundedDrawable(
            this@MainActivity,
            ColorType.Selector(pressedColor, defaultColor),
            ShapeStyle.Stroke(strokeWidth),
            ShapeType.Rect(strokeRadius),
        )
        frame.clipToOutline = true
        frame.foreground = stroke
        frame.outlineProvider = stroke.outlineProvider

        if (isCurvedOutlineWork) {
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
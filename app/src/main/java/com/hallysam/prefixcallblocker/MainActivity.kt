package com.hallysam.prefixcallblocker

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var repository: PrefixRepository
    private lateinit var statusText: TextView
    private lateinit var setupButton: Button
    private lateinit var blockingSwitch: SwitchMaterial
    private lateinit var prefixContainer: LinearLayout
    private lateinit var historyContainer: LinearLayout
    private lateinit var historySection: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = PrefixRepository(this)
        setContentView(buildUi())
        observeData()
    }

    override fun onResume() {
        super.onResume()
        updateScreeningStatus()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(32))
        }

        val title = TextView(this).apply {
            text = "Prefix Call Blocker"
            textSize = 28f
            setTextColor(ContextCompat.getColor(context, R.color.primary_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(title, lp())

        val subtitle = TextView(this).apply {
            text = "Block unwanted incoming calls by number prefix."
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
        }
        root.addView(subtitle, lp(0, 4, 0, 18))

        val blockingCard = card()
        val blockingRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(TextView(this).apply {
            text = "Call blocking"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.primary_text))
        }, lp())
        labels.addView(TextView(this).apply {
            text = "Automatically reject matching calls"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
        }, lp(0, 3, 0, 0))

        blockingSwitch = SwitchMaterial(this)
        blockingSwitch.setOnCheckedChangeListener { _, checked ->
            lifecycleScope.launch { repository.setBlockingEnabled(checked) }
        }

        blockingRow.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        blockingRow.addView(blockingSwitch)
        blockingCard.addView(blockingRow)
        root.addView(blockingCard, lp(0, 0, 0, 14))

        val statusCard = card()
        statusText = TextView(this).apply {
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.primary_text))
        }
        setupButton = Button(this).apply {
            text = "Set as Call Screening App"
            isAllCaps = false
            setOnClickListener { requestCallScreeningRole() }
        }
        statusCard.addView(statusText, lp())
        statusCard.addView(setupButton, lp(0, 12, 0, 0))
        root.addView(statusCard, lp(0, 0, 0, 20))

        root.addView(sectionTitle("Blocked prefixes"))
        prefixContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(prefixContainer, lp(0, 8, 0, 10))

        val addButton = Button(this).apply {
            text = "+ Add Prefix"
            isAllCaps = false
            setOnClickListener { showAddPrefixDialog() }
        }
        root.addView(addButton, lp(0, 0, 0, 20))

        root.addView(sectionTitle("Blocked Calls"))
        historySection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        historyContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        historySection.addView(historyContainer)
        val clearButton = Button(this).apply {
            text = "Clear History"
            isAllCaps = false
            setOnClickListener {
                lifecycleScope.launch { repository.clearHistory() }
            }
        }
        historySection.addView(clearButton, lp(0, 8, 0, 0))
        root.addView(historySection, lp(0, 8, 0, 20))

        root.addView(sectionTitle("About"))
        root.addView(TextView(this).apply {
            text = "Prefix Call Blocker\n\nBlock unwanted calls by prefix.\nVersion 1.0\n\nAll prefixes and blocked-call history stay on this device. No account, server, analytics, or internet connection is required."
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
        }, lp(0, 8, 0, 0))

        scroll.addView(root)
        return scroll
    }

    private fun observeData() {
        lifecycleScope.launch {
            repository.blockingEnabled.collectLatest { blockingSwitch.isChecked = it }
        }
        lifecycleScope.launch {
            repository.prefixes.collectLatest { renderPrefixes(it) }
        }
        lifecycleScope.launch {
            repository.history.collectLatest { renderHistory(it) }
        }
    }

    private fun renderPrefixes(prefixes: Set<String>) {
        prefixContainer.removeAllViews()
        if (prefixes.isEmpty()) {
            prefixContainer.addView(TextView(this).apply {
                text = "No prefixes saved. All calls will be allowed."
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
                setPadding(dp(4), dp(8), dp(4), dp(8))
            })
            return
        }

        prefixes.sorted().forEach { prefix ->
            val row = card()
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            line.addView(TextView(this).apply {
                text = prefix
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.primary_text))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val delete = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                contentDescription = "Delete $prefix"
                background = null
                setOnClickListener {
                    lifecycleScope.launch { repository.deletePrefix(prefix) }
                }
            }
            line.addView(delete)
            row.addView(line)
            prefixContainer.addView(row, lp(0, 0, 0, 8))
        }
    }

    private fun renderHistory(history: List<BlockedCall>) {
        historyContainer.removeAllViews()
        if (history.isEmpty()) {
            historyContainer.addView(TextView(this).apply {
                text = "No blocked calls yet."
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
            })
            return
        }

        history.take(20).forEach { item ->
            val row = TextView(this).apply {
                text = "${item.number}\nPrefix matched: ${item.prefix}\n${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date(item.timestamp))}"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.primary_text))
                setPadding(dp(4), dp(10), dp(4), dp(10))
            }
            historyContainer.addView(row, lp())
        }
    }

    private fun updateScreeningStatus() {
        val active = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }

        if (active) {
            statusText.text = "Call Screening:\n✓ Active"
            setupButton.visibility = View.GONE
        } else {
            statusText.text = "Call Screening:\n⚠ Setup required"
            setupButton.visibility = View.VISIBLE
        }
    }

    private fun requestCallScreeningRole() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true) {
                startActivityForResult(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                    ROLE_REQUEST
                )
                return
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun showAddPrefixDialog() {
        val inputLayout = TextInputLayout(this).apply {
            hint = "Prefix"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val input = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            singleLine = true
        }
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add blocked prefix")
            .setView(inputLayout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val raw = input.text?.toString().orEmpty()
                val normalized = PhoneNumberUtils.normalizePrefix(raw)
                if (normalized == null) {
                    Toast.makeText(this, "Enter a valid prefix.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val existing = repository.getPrefixes()
                    if (existing.contains(normalized)) {
                        Toast.makeText(this@MainActivity, "That prefix already exists.", Toast.LENGTH_SHORT).show()
                    } else {
                        repository.addPrefix(normalized)
                        Toast.makeText(this@MainActivity, "Prefix added.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(ContextCompat.getColor(context, R.color.primary_text))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_card)
    }

    private fun lp(
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0
    ) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(dp(left), dp(top), dp(right), dp(bottom))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ROLE_REQUEST = 1001
    }
}

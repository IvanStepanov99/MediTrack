package com.example.medtracker.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.medtracker.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator

class AddDrugDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_LABEL = "arg_label"
        private const val ARG_GENERIC = "arg_generic"
        private const val ARG_BRAND = "arg_brand"
        const val RESULT_KEY = "add_drug"
        private const val SELECTED_INDEX_KEY = "selected_index"
        private const val SELECTED_FREQ_KEY = "selected_freq"
        private const val SELECTED_WEEKDAYS_KEY = "selected_weekdays"

        // Factory to create the dialog with arguments (label, generic, brand)
        fun newInstance(label: String, generic: String?, brand: String?) = AddDrugDialogFragment().apply {
            arguments = bundleOf(ARG_LABEL to label, ARG_GENERIC to generic, ARG_BRAND to brand)
        }
    }

    private lateinit var tvDrugName: TextView
    private lateinit var tvDrugNamePage2: TextView
    private lateinit var tvDrugNamePage3: TextView
    private lateinit var recyclerForm: androidx.recyclerview.widget.RecyclerView
    private lateinit var recyclerUnit: androidx.recyclerview.widget.RecyclerView
    private lateinit var page1: View
    private lateinit var page2: View
    private lateinit var page3: View
    private lateinit var pagesContainer: ViewGroup
    private lateinit var etStrength: TextInputEditText
    private lateinit var buttonBarPage1: View
    private lateinit var buttonBarPage2: View
    private lateinit var buttonBarPage3: View
    private lateinit var btnNext: Button
    private lateinit var btnNextPage2: Button
    private lateinit var btnBack: Button
    private lateinit var btnBackPage3: Button
    private lateinit var btnCancel: Button
    private lateinit var btnAdd: Button
    private lateinit var formAdapter: FormAdapter
    private lateinit var tilUnit: TextInputLayout
    private lateinit var unitAdapter: UnitAdapter

    // Page3 controls
    private lateinit var rgFrequency: RadioGroup
    private lateinit var chipGroupWeekDays: ChipGroup

    // holds the selected form (null if not selected)
    private var selectedForm: String? = null
    private var selectedIndex: Int = -1
    private var selectedUnitIndex: Int = -1
    private var selectedFreqId: Int = -1
    private var selectedWeekDays: MutableSet<String> = mutableSetOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // use transparent window background
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val v = inflater.inflate(R.layout.dialog_add_drug_multi, container, false)

        tvDrugName = v.findViewById(R.id.tvDrugName)
        tvDrugNamePage2 = v.findViewById(R.id.tvDrugNamePage2)
        tvDrugNamePage3 = v.findViewById(R.id.tvDrugNamePage3)
        // RecyclerView for form list (defined in layout)
        recyclerForm = v.findViewById(R.id.recyclerForm)
        // RecyclerView for unit list
        recyclerUnit = v.findViewById(R.id.recyclerUnit)
        page1 = v.findViewById(R.id.page1)
        page2 = v.findViewById(R.id.page2)
        page3 = v.findViewById(R.id.page3)
        pagesContainer = v.findViewById(R.id.pagesContainer)
        etStrength = v.findViewById(R.id.etStrength)
        buttonBarPage1 = v.findViewById(R.id.buttonBarPage1)
        // new: wrappers for showing inline errors
        val tilStrength = v.findViewById<TextInputLayout>(R.id.tilStrength)
        tilUnit = v.findViewById<TextInputLayout>(R.id.tilUnit)

        btnCancel = v.findViewById<Button>(R.id.btnCancel)
        btnNext = v.findViewById<Button>(R.id.btnNext)
        btnNextPage2 = v.findViewById(R.id.btnNextPage2)
        btnBack = v.findViewById<Button>(R.id.btnBack)
        btnBackPage3 = v.findViewById(R.id.btnBackPage3)
        btnAdd = v.findViewById<Button>(R.id.btnAdd)
        // newly added second and third button bars
        buttonBarPage2 = v.findViewById(R.id.buttonBarPage2)
        buttonBarPage3 = v.findViewById(R.id.buttonBarPage3)

        // Page3 controls
        rgFrequency = v.findViewById(R.id.rgFrequencyOptions)
        chipGroupWeekDays = v.findViewById(R.id.chipGroupWeekDays)

        val label = arguments?.getString(ARG_LABEL).orEmpty()
        val generic = arguments?.getString(ARG_GENERIC)
        val brand = arguments?.getString(ARG_BRAND)

        // Show brand if present, otherwise generic, otherwise the label
        val display = brand?.takeIf { it.isNotBlank() } ?: generic?.takeIf { it.isNotBlank() } ?: label
        tvDrugName.text = display

        // keep page2/page3 drug name in sync
        tvDrugNamePage2.text = display
        tvDrugNamePage3.text = display

        // Populate form list from string-array resource and wire RecyclerView
        val forms = resources.getStringArray(R.array.form_options).toList()
        formAdapter = FormAdapter(forms)
        recyclerForm.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerForm.adapter = formAdapter

        // Populate unit list from CSV string in strings.xml and wire RecyclerView
        val unitsCsv = resources.getString(R.string.unit_options_csv)
        val units = unitsCsv.split("|").map { it.trim() }
        unitAdapter = UnitAdapter(units)
        recyclerUnit.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerUnit.adapter = unitAdapter

        // ensure initial visibility: show page1 and the bottom bar
        buttonBarPage1.visibility = View.VISIBLE
        buttonBarPage2.visibility = View.GONE
        buttonBarPage3.visibility = View.GONE
        page1.visibility = View.VISIBLE
        page2.visibility = View.GONE
        page3.visibility = View.GONE

        // Restore selection if available (use -1 for no selection)
        if (savedInstanceState != null) {
            selectedIndex = savedInstanceState.getInt(SELECTED_INDEX_KEY, -1)
            selectedForm = if (selectedIndex >= 0) forms.getOrNull(selectedIndex) else null
            selectedUnitIndex = savedInstanceState.getInt(SELECTED_INDEX_KEY + "_unit", -1)
            selectedFreqId = savedInstanceState.getInt(SELECTED_FREQ_KEY, -1)
            val savedDays = savedInstanceState.getStringArrayList(SELECTED_WEEKDAYS_KEY) ?: arrayListOf()
            selectedWeekDays = savedDays.toMutableSet()

            // restore unit adapter selection and scroll
            val prevUnit = unitAdapter.selectedIndex
            unitAdapter.selectedIndex = selectedUnitIndex
            if (prevUnit != selectedUnitIndex) {
                if (prevUnit in 0 until unitAdapter.itemCount) unitAdapter.notifyItemChanged(prevUnit)
                if (selectedUnitIndex in 0 until unitAdapter.itemCount) unitAdapter.notifyItemChanged(selectedUnitIndex)
            } else if (selectedUnitIndex in 0 until unitAdapter.itemCount) {
                recyclerUnit.scrollToPosition(selectedUnitIndex)
            }

            // restore frequency selection and chip state
            if (selectedFreqId != -1) rgFrequency.check(selectedFreqId)
            // ensure chip visibility accordingly
            chipGroupWeekDays.visibility = if (rgFrequency.checkedRadioButtonId == R.id.rbSpecificWeekDay) View.VISIBLE else View.GONE
            // restore checked chips
            for (i in 0 until chipGroupWeekDays.childCount) {
                val child = chipGroupWeekDays.getChildAt(i)
                if (child is Chip) {
                    child.isChecked = selectedWeekDays.contains(child.text.toString().uppercase())
                }
            }
        }

        // listen for frequency changes (show weekday chips only when specific-week selected)
        rgFrequency.setOnCheckedChangeListener { _, checkedId ->
            chipGroupWeekDays.visibility = if (checkedId == R.id.rbSpecificWeekDay) View.VISIBLE else View.GONE
            selectedFreqId = checkedId
        }

        // manage chip selection set
        for (i in 0 until chipGroupWeekDays.childCount) {
            val child = chipGroupWeekDays.getChildAt(i)
            if (child is Chip) {
                child.setOnCheckedChangeListener { chip, isChecked ->
                    val code = chip.text.toString().trim().uppercase()
                    if (isChecked) selectedWeekDays.add(code) else selectedWeekDays.remove(code)
                }
            }
        }

        // clear inline errors when user edits strength
        etStrength.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilStrength.error = null
            }
        })

        // Unit selection is handled by the UnitAdapter; adapter clears tilUnit.error when selection occurs

        btnCancel.setOnClickListener { dismiss() }

        btnNext.setOnClickListener {
            val selected = selectedForm?.trim().orEmpty()
            if (selected.isEmpty()) {
                // show an inline-friendly message
                Toast.makeText(requireContext(), "Please choose a form", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // ensure page2 shows the same drug name
            tvDrugNamePage2.text = display
            // disable navigation to prevent double taps
            btnNext.isEnabled = false
            btnCancel.isEnabled = false
            // animate to page2
            animateToPage2()
        }

        // page2 back -> page1
        btnBack.setOnClickListener {
            // disable to avoid double taps
            btnBack.isEnabled = false
            animateToPage1()
        }

        // page2 next -> page3 (validate strength/unit first)
        btnNextPage2.setOnClickListener {
            if (!validateStrengthUnit()) return@setOnClickListener
            // navigate to page3
            btnNextPage2.isEnabled = false
            animateToPage3()
        }

        // page3 back -> page2
        btnBackPage3.setOnClickListener {
            btnBackPage3.isEnabled = false
            animateBackToPage2()
        }

        // Add button (final) - validate and return result including schedule info
        btnAdd.setOnClickListener {
            // validate required fields (strength/unit)
            if (!validateStrengthUnit()) {
                // keep user on page2/page3 and show errors
                return@setOnClickListener
            }

            // determine schedule selections
            var prn = false
            var freqType = ""
            var byWeekDayList: ArrayList<String>? = null

            when (rgFrequency.checkedRadioButtonId) {
                R.id.rbAsNeeded -> {
                    prn = true
                    freqType = "PRN"
                }
                R.id.rbEveryDay -> {
                    prn = false
                    freqType = "DAILY"
                }
                R.id.rbSpecificWeekDay -> {
                    prn = false
                    freqType = "WEEKLY"
                    // collect selected chips
                    val sel = arrayListOf<String>()
                    for (i in 0 until chipGroupWeekDays.childCount) {
                        val child = chipGroupWeekDays.getChildAt(i)
                        if (child is Chip && child.isChecked) {
                            // use two-letter codes consistent with DB tests (MO, TU, ...)
                            sel.add(child.text.toString().trim().uppercase())
                        }
                    }
                    if (sel.isEmpty()) {
                        Toast.makeText(requireContext(), "Select at least one weekday", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    byWeekDayList = sel
                }
                else -> {
                    Toast.makeText(requireContext(), "Choose a schedule option", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val form = selectedForm.orEmpty().trim()
            val strength = etStrength.text?.toString().orEmpty().toDoubleOrNull() ?: Double.NaN
            val unit = if (selectedUnitIndex >= 0 && selectedUnitIndex < units.size) units[selectedUnitIndex] else ""

            val result = bundleOf(
                "label" to display,
                "form" to form,
                "strength" to strength,
                "unit" to unit,
                "generic" to generic,
                "brand" to brand,
                // schedule fields
                "prn" to prn,
                "freqType" to freqType,
                "byWeekDay" to byWeekDayList
            )

            parentFragmentManager.setFragmentResult(RESULT_KEY, result)
            dismiss()
        }

        return v
    }

    // Simple RecyclerView adapter for the list of forms
    private inner class FormAdapter(private val items: List<String>) : androidx.recyclerview.widget.RecyclerView.Adapter<FormAdapter.VH>() {
        // -1 means nothing selected (useful so placeholder at index 0 is not activated by default)
        var selectedIndex: Int = -1

        inner class VH(val textView: TextView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_activated_1, parent, false)
            return VH(v.findViewById(android.R.id.text1))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val text = items[position]
            holder.textView.text = text
            // visual selection: selected item gets purple background and white text
            val isSelected = position == selectedIndex
            if (isSelected) {
                holder.itemView.setBackgroundColor(Color.parseColor("#5959DF"))
                holder.textView.setTextColor(Color.WHITE)
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                holder.textView.setTextColor(Color.parseColor("#333333"))
            }
            holder.itemView.isActivated = isSelected
            holder.itemView.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return@setOnClickListener
                val prev = selectedIndex
                if (prev == pos) return@setOnClickListener
                selectedIndex = pos
                // update fragment state (no placeholder now — every index is a real option)
                this@AddDrugDialogFragment.selectedIndex = pos
                this@AddDrugDialogFragment.selectedForm = items[pos]
                if (prev in 0 until itemCount) notifyItemChanged(prev)
                notifyItemChanged(pos)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    // Adapter for unit list
    private inner class UnitAdapter(private val items: List<String>) : androidx.recyclerview.widget.RecyclerView.Adapter<UnitAdapter.VH>() {
        var selectedIndex: Int = -1

        inner class VH(val textView: TextView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_activated_1, parent, false)
            return VH(v.findViewById(android.R.id.text1))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val text = items[position]
            holder.textView.text = text
            val isSelected = position == selectedIndex
            if (isSelected) {
                holder.itemView.setBackgroundColor(Color.parseColor("#5959DF"))
                holder.textView.setTextColor(Color.WHITE)
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                holder.textView.setTextColor(Color.parseColor("#333333"))
            }
            holder.itemView.isActivated = isSelected
            holder.itemView.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return@setOnClickListener
                val prev = selectedIndex
                if (prev == pos) return@setOnClickListener
                selectedIndex = pos
                // update fragment state
                selectedUnitIndex = pos
                selectedUnitIndex = pos
                selectedForm // no-op just to reference
                tilUnit.error = null
                if (prev in 0 until itemCount) notifyItemChanged(prev)
                notifyItemChanged(pos)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    // Move animation helpers to class-level so click listeners can always reference them
    private fun animateToPage2() {
        val w = (pagesContainer.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels).toFloat()

        // prepare page2
        page2.translationX = w
        page2.alpha = 0f
        page2.visibility = View.VISIBLE

        // animate page1 out to left
        page1.animate()
            .translationX(-w)
            .alpha(0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                page1.visibility = View.GONE
                page1.translationX = 0f
                page1.alpha = 1f
                btnNext.isEnabled = true
                btnCancel.isEnabled = true
            }
            .start()

        // animate page2 in from right
        page2.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // fade out page1 bottom bar and fade in page2 bottom bar
        buttonBarPage1.animate().alpha(0f).setDuration(180).withEndAction {
            buttonBarPage1.visibility = View.GONE
            buttonBarPage1.alpha = 1f
            // show page2 bar
            buttonBarPage2.alpha = 0f
            buttonBarPage2.visibility = View.VISIBLE
            buttonBarPage2.animate().alpha(1f).setDuration(180).start()
            // enable page2 buttons
            btnBack.isEnabled = true
            btnNextPage2.isEnabled = true
        }.start()
    }

    private fun animateToPage3() {
        val w = (pagesContainer.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels).toFloat()

        // prepare page3
        page3.translationX = w
        page3.alpha = 0f
        page3.visibility = View.VISIBLE

        // animate page2 out to left
        page2.animate()
            .translationX(-w)
            .alpha(0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                page2.visibility = View.GONE
                page2.translationX = 0f
                page2.alpha = 1f
                btnNextPage2.isEnabled = true
            }
            .start()

        // animate page3 in from right
        page3.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // fade out page2 bottom bar and fade in page3 bottom bar
        buttonBarPage2.animate().alpha(0f).setDuration(180).withEndAction {
            buttonBarPage2.visibility = View.GONE
            buttonBarPage2.alpha = 1f
            // show page3 bar
            buttonBarPage3.alpha = 0f
            buttonBarPage3.visibility = View.VISIBLE
            buttonBarPage3.animate().alpha(1f).setDuration(180).start()
            btnBackPage3.isEnabled = true
            btnAdd.isEnabled = true
        }.start()
    }

    private fun animateBackToPage2() {
        val w = (pagesContainer.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels).toFloat()

        // prepare page2 to come in from left
        page2.translationX = -w
        page2.alpha = 0f
        page2.visibility = View.VISIBLE

        // animate page3 out to right
        page3.animate()
            .translationX(w)
            .alpha(0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                page3.visibility = View.GONE
                page3.translationX = 0f
                page3.alpha = 1f
                btnBackPage3.isEnabled = true
                btnCancel.isEnabled = true
            }
            .start()

        // animate page2 in
        page2.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // fade out page3 bar and fade in page2 bar
        buttonBarPage3.animate().alpha(0f).setDuration(180).withEndAction {
            buttonBarPage3.visibility = View.GONE
            buttonBarPage3.alpha = 1f
            // reveal page2 bar
            buttonBarPage2.alpha = 0f
            buttonBarPage2.visibility = View.VISIBLE
            buttonBarPage2.animate().alpha(1f).setDuration(180).withEndAction { btnNextPage2.isEnabled = true }.start()
        }.start()
    }

    private fun animateToPage1() {
        val w = (pagesContainer.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels).toFloat()

        // prepare page1 to come in from left
        page1.translationX = -w
        page1.alpha = 0f
        page1.visibility = View.VISIBLE

        // animate page2 out to right
        page2.animate()
            .translationX(w)
            .alpha(0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                page2.visibility = View.GONE
                page2.translationX = 0f
                page2.alpha = 1f
                // re-enable navigation now that transition finished
                btnBack.isEnabled = true
                btnCancel.isEnabled = true
            }
            .start()

        // animate page1 in
        page1.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // fade out page2 bar and fade in page1 bar
        buttonBarPage2.animate().alpha(0f).setDuration(180).withEndAction {
            buttonBarPage2.visibility = View.GONE
            buttonBarPage2.alpha = 1f
            // reveal page1 bar
            buttonBarPage1.alpha = 0f
            buttonBarPage1.visibility = View.VISIBLE
            buttonBarPage1.animate().alpha(1f).setDuration(180).withEndAction { btnNext.isEnabled = true }.start()
        }.start()
    }

    // Validate strength and unit; reused by navigation and final Add button
    private fun validateStrengthUnit(): Boolean {
        var ok = true
        val sText = etStrength.text?.toString()?.trim().orEmpty()
        val unitPosition = selectedUnitIndex

        if (sText.isEmpty()) {
            // locate tilStrength safely
            val tilStrengthLocal = view?.findViewById<TextInputLayout>(R.id.tilStrength)
            tilStrengthLocal?.error = "Required"
            ok = false
        } else {
            val d = sText.toDoubleOrNull()
            val tilStrengthLocal = view?.findViewById<TextInputLayout>(R.id.tilStrength)
            if (d == null) {
                tilStrengthLocal?.error = "Invalid number"
                ok = false
            } else {
                tilStrengthLocal?.error = null
            }
        }

        if (unitPosition < 0) {
            val tilUnitLocal = view?.findViewById<TextInputLayout>(R.id.tilUnit)
            tilUnitLocal?.error = "Required"
            ok = false
        } else {
            val tilUnitLocal = view?.findViewById<TextInputLayout>(R.id.tilUnit)
            tilUnitLocal?.error = null
        }

        return ok
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_INDEX_KEY + "_unit", selectedUnitIndex)
        outState.putInt(SELECTED_INDEX_KEY, selectedIndex)
        outState.putInt(SELECTED_FREQ_KEY, selectedFreqId)
        outState.putStringArrayList(SELECTED_WEEKDAYS_KEY, ArrayList(selectedWeekDays))
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { w ->
            // transparent background to allow rounded card corners
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.98).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.5).toInt()
            w.setLayout(width, height)
            w.setGravity(Gravity.CENTER)
            val params = w.attributes
            params.width = width
            params.height = height
            params.gravity = Gravity.CENTER
            w.attributes = params
            w.setDimAmount(0.5f)
        }
    }
}

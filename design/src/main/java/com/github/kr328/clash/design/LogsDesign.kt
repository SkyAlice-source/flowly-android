package com.github.kr328.clash.design

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.github.kr328.clash.design.adapter.LogFileAdapter
import com.github.kr328.clash.design.databinding.DesignLogsBinding
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.design.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class LogsDesign(context: Context) : Design<LogsDesign.Request>(context) {
    sealed class Request {
        object StartLogcat : Request()
        object DeleteAll : Request()

        data class OpenFile(val file: LogFile) : Request()
    }

    private val binding = DesignLogsBinding
        .inflate(context.layoutInflater, context.root, false)
    private val adapter = LogFileAdapter(context) {
        requests.trySend(Request.OpenFile(it))
    }

    private var allLogs: List<LogFile> = emptyList()

    override val root: View
        get() = binding.root

    suspend fun patchLogs(logs: List<LogFile>) {
        allLogs = logs
        applyFilter(binding.logSearchInput.text?.toString().orEmpty())
    }

    private fun applyFilter(query: String) {
        val filtered = if (query.isBlank()) {
            allLogs
        } else {
            val q = query.lowercase()
            allLogs.filter { it.fileName.lowercase().contains(q) }
        }

        binding.logsEmptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerList.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

        adapter.logs = filtered
        adapter.notifyDataSetChanged()
    }

    suspend fun requestDeleteAll(): Boolean {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { ctx ->
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.delete_all_logs)
                    .setMessage(R.string.delete_all_logs_warn)
                    .setPositiveButton(R.string.ok) { _, _ -> ctx.resume(true) }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()
                    .setOnDismissListener { if (!ctx.isCompleted) ctx.resume(false) }
            }
        }
    }

    init {
        binding.self = this

        binding.activityBarLayout.applyFrom(context)

        binding.recyclerList.applyLinearAdapter(context, adapter)

        binding.logSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter(s?.toString().orEmpty()) }
        })
    }
}
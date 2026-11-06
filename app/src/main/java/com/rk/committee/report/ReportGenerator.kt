package com.rk.committee.report

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.rk.committee.R
import com.rk.committee.domain.CommitteeDetails
import com.rk.committee.ui.util.formatMonth
import com.rk.committee.ui.util.formatNumber
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Builds a simple A4 PDF report. Pass an Activity context so texts use the app language.
 */
object ReportGenerator {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN

    private val GREEN = Color.rgb(11, 110, 79)
    private val RED = Color.rgb(186, 26, 26)
    private val GRAY = Color.rgb(112, 121, 115)
    private val LIGHT = Color.rgb(232, 240, 234)

    fun create(context: Context, d: CommitteeDetails): File {
        val locale = context.resources.configuration.locales[0]
        fun money(v: Long) = context.getString(R.string.currency_format, formatNumber(v, locale))
        fun month(r: Int) = formatMonth(d.monthOf(r), locale)

        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD; color = GREEN }
        val h2 = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.BLACK }
        val bold = TextPaint(body).apply { typeface = Typeface.DEFAULT_BOLD }
        val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = GRAY }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val line = Paint().apply { color = LIGHT; strokeWidth = 1f }

        val doc = PdfDocument()
        val w = PageWriter(doc)

        fun text(s: String, x: Float, paint: TextPaint, width: Float) {
            val t = TextUtils.ellipsize(s, paint, width, TextUtils.TruncateAt.END).toString()
            w.canvas.drawText(t, x, w.y, paint)
        }

        // ---- Header
        text(context.getString(R.string.app_name), MARGIN, small, CONTENT_W); w.y += 22f
        text(d.committee.name, MARGIN, title, CONTENT_W); w.y += 16f
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
        text(context.getString(R.string.report_generated_on, today), MARGIN, small, CONTENT_W); w.y += 20f

        // ---- Summary
        text(context.getString(R.string.summary), MARGIN, h2, CONTENT_W); w.y += 16f
        val status = when {
            d.isCompleted -> context.getString(R.string.committee_completed)
            !d.isStarted -> context.getString(R.string.starts_on, formatMonth(d.startMonth, locale))
            else -> context.getString(R.string.month_x_of_y, d.currentRound + 1, d.totalRounds) + " · " + month(d.currentRound)
        }
        val next = d.nextTurnRound
        val rows = listOf(
            context.getString(R.string.amount_per_member) to money(d.committee.amountPerMember),
            context.getString(R.string.members) to "${d.totalRounds}",
            context.getString(R.string.pot_per_month) to money(d.potAmount),
            context.getString(R.string.start_month) to formatMonth(d.startMonth, locale),
            context.getString(R.string.status) to status,
            context.getString(R.string.total_collected) to money(d.totalCollected),
            context.getString(R.string.next_turn) to
                (next?.let { "${d.receiverFor(it)?.name.orEmpty()} (${month(it)})" } ?: "-"),
        )
        rows.forEach { (k, v) ->
            text(k, MARGIN, body, 170f)
            text(v, MARGIN + 180f, bold, CONTENT_W - 180f)
            w.y += 15f
        }
        w.y += 12f

        // ---- Members table
        w.ensure(60f)
        text(context.getString(R.string.tab_members), MARGIN, h2, CONTENT_W); w.y += 10f
        val cols = floatArrayOf(24f, 130f, 92f, 80f, 48f, 56f, 85f) // = 515
        val heads = listOf(
            "#",
            context.getString(R.string.member_name),
            context.getString(R.string.phone),
            context.getString(R.string.turn_month),
            context.getString(R.string.paid),
            context.getString(R.string.pending),
            context.getString(R.string.pot_received),
        )
        fun tableRow(values: List<String>, paint: TextPaint, bg: Int?) {
            w.ensure(20f)
            if (bg != null) {
                fill.color = bg
                w.canvas.drawRect(MARGIN, w.y, MARGIN + CONTENT_W, w.y + 18f, fill)
            }
            var x = MARGIN + 4f
            w.y += 13f
            values.forEachIndexed { i, v ->
                text(v, x, paint, cols[i] - 6f)
                x += cols[i]
            }
            w.y += 5f
            w.canvas.drawLine(MARGIN, w.y, MARGIN + CONTENT_W, w.y, line)
        }
        tableRow(heads, bold, LIGHT)
        d.members.forEachIndexed { i, m ->
            tableRow(
                listOf(
                    "${i + 1}",
                    m.name,
                    m.phone.ifBlank { "-" },
                    month(i),
                    "${d.paidCount(m.id)}",
                    "${d.pendingRounds(m.id)}",
                    if (d.payoutFor(i) != null) context.getString(R.string.yes) else context.getString(R.string.no),
                ),
                body,
                null,
            )
        }
        w.y += 18f

        // ---- Payment grid (one column per month)
        val n = d.totalRounds
        if (n > 0) {
            w.ensure(80f)
            text(context.getString(R.string.payment_record), MARGIN, h2, CONTENT_W); w.y += 6f
            val nameW = 110f
            val cell = (CONTENT_W - nameW) / n
            w.y += 12f
            for (r in 0 until n) {
                val label = "${r + 1}"
                w.canvas.drawText(label, MARGIN + nameW + r * cell + cell / 2 - small.measureText(label) / 2, w.y, small)
            }
            w.y += 6f
            val dueUntil = if (d.isStarted) d.currentRound else -1
            d.members.forEach { m ->
                w.ensure(16f)
                w.y += 11f
                text(m.name, MARGIN, body, nameW - 6f)
                val cy = w.y - 3.5f
                for (r in 0 until n) {
                    val cx = MARGIN + nameW + r * cell + cell / 2
                    val radius = minOf(4f, cell / 2 - 1f)
                    when {
                        d.isPaid(m.id, r) -> { fill.color = GREEN; fill.style = Paint.Style.FILL }
                        r <= dueUntil -> { fill.color = RED; fill.style = Paint.Style.STROKE; fill.strokeWidth = 1.2f }
                        else -> { fill.color = LIGHT; fill.style = Paint.Style.FILL }
                    }
                    w.canvas.drawCircle(cx, cy, radius, fill)
                }
                fill.style = Paint.Style.FILL
                w.y += 5f
            }
            w.y += 10f
            text(context.getString(R.string.grid_legend), MARGIN, small, CONTENT_W)
            w.y += 20f
        }

        // ---- Unpaid this month
        if (d.isStarted && !d.isCompleted) {
            val unpaid = d.unpaidMembers(d.currentRound)
            w.ensure(40f)
            text(context.getString(R.string.this_month_title, month(d.currentRound)), MARGIN, h2, CONTENT_W); w.y += 16f
            val msg = if (unpaid.isEmpty()) context.getString(R.string.all_paid)
            else context.getString(R.string.not_paid_yet) + " " + unpaid.joinToString(", ") { it.name }
            text(msg, MARGIN, body, CONTENT_W)
        }

        w.finish()

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safe = d.committee.name.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40).ifBlank { "committee" }
        val file = File(dir, "${safe}_report.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_report)))
    }

    /** Keeps track of the current page and y position; adds pages when needed. */
    private class PageWriter(private val doc: PdfDocument) {
        private var pageNo = 0
        private lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = 0f

        init { newPage() }

        fun newPage() {
            if (pageNo > 0) doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensure(space: Float) {
            if (y + space > PAGE_H - MARGIN) newPage()
        }

        fun finish() = doc.finishPage(page)
    }
}

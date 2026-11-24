package com.easygit.mobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var repoUrl: EditText
    private lateinit var branch: EditText
    private lateinit var token: EditText
    private lateinit var commitMessage: EditText
    private lateinit var repoPath: TextView
    private lateinit var status: TextView
    private lateinit var log: TextView
    private var repoDir: File? = null
    private var importTreeUri: Uri? = null

    companion object {
        private const val PICK_IMPORT = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        val savedPath = getSharedPreferences("easy_git", MODE_PRIVATE).getString("repo_path", null)
        if (savedPath != null) {
            val savedRepo = File(savedPath)
            if (savedRepo.exists()) {
                repoDir = savedRepo
                repoPath.text = savedRepo.absolutePath
            }
        }
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(column)
        setContentView(scroll)

        column.addView(TextView(this).apply {
            text = "Easy Git"
            textSize = 27f
            setTextColor(0xFF202124.toInt())
            setPadding(0, 0, 0, dp(6))
        })
        column.addView(TextView(this).apply {
            text = "مدیریت مخزن GitHub از گوشی"
            textSize = 15f
            setTextColor(0xFF666666.toInt())
        })

        repoUrl = field(column, "نشانی HTTPS مخزن (مثلاً https://github.com/user/repo.git)")
        branch = field(column, "شاخه (Branch)", "main")
        token = field(column, "GitHub Personal Access Token", "").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        commitMessage = field(column, "پیام Commit", "Update from Easy Git")

        section(column, "مخزن محلی")
        repoPath = TextView(this).apply {
            text = "هنوز مخزنی انتخاب یا Clone نشده است"
            textSize = 13f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(6), 0, dp(10))
        }
        column.addView(repoPath)

        row(column,
            button("نمایش مسیر مخزن") {
                report(repoDir?.absolutePath ?: "هنوز مخزنی Clone نشده است.")
            },
            button("Clone از GitHub") { cloneRepository() }
        )
        section(column, "انتقال فایل‌ها و پوشه‌ها")
        column.addView(button("انتخاب پوشه برای واردکردن") { chooseImportFolder() })
        column.addView(button("واردکردن پوشه به مخزن") { importFolder() })
        section(column, "عملیات Git")
        row(column,
            button("Commit + Push") { commitAndPush() },
            button("Pull") { pull() }
        )
        row(column,
            button("وضعیت مخزن") { showStatus() },
            button("نمایش تاریخچه") { showHistory() }
        )
        column.addView(button("خروجی ZIP از مخزن") { exportZip() })

        section(column, "گزارش عملیات")
        status = TextView(this).apply {
            text = "آماده"
            textSize = 14f
            setTextColor(0xFF1565C0.toInt())
            setPadding(0, dp(8), 0, dp(8))
        }
        log = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFF333333.toInt())
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(0xFFF3F4F6.toInt())
            text = "برای شروع، نشانی مخزن و توکن GitHub را وارد کنید."
        }
        column.addView(status)
        column.addView(log)
    }

    private fun field(parent: LinearLayout, hintText: String, value: String = ""): EditText {
        val edit = EditText(this).apply {
            hint = hintText
            setText(value)
            textSize = 14f
            setSingleLine(true)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        parent.addView(edit, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ).apply { bottomMargin = dp(4) })
        return edit
    }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(0xFF202124.toInt())
            setPadding(0, dp(18), 0, dp(6))
        })
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 13f
        setOnClickListener { action() }
    }

    private fun row(parent: LinearLayout, a: View, b: View) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(a, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(5) })
            addView(b, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(5) })
        })
    }

    private fun chooseImportFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, PICK_IMPORT)
    }

    @Deprecated("Using Activity Result API is optional for this starter")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return
        contentResolver.takePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (requestCode == PICK_IMPORT) {
            importTreeUri = uri
            report("پوشه ورودی انتخاب شد.")
        }
    }

    private fun cloneRepository() {
        val url = repoUrl.text.toString().trim()
        val dest = repoDir ?: run {
            val name = url.substringAfterLast('/').removeSuffix(".git").ifBlank { "repository" }
            File(getExternalFilesDir(null) ?: filesDir, "repositories/$name").also { repoDir = it }
        }
        if (url.isBlank()) { report("نشانی مخزن را وارد کنید."); return }
        runGit("در حال Clone...") {
            if (dest.exists() && dest.list()?.isNotEmpty() == true) {
                throw IllegalStateException("پوشه مقصد خالی نیست. یک پوشه خالی انتخاب کنید.")
            }
            dest.parentFile?.mkdirs()
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(dest)
                .setBranch(branch.text.toString().trim().ifBlank { "main" })
                .setCredentialsProvider(credentials())
                .call().close()
            getSharedPreferences("easy_git", MODE_PRIVATE).edit()
                .putString("repo_path", dest.absolutePath).apply()
            runOnUiThread { repoPath.text = dest.absolutePath }
            "Clone با موفقیت انجام شد: ${dest.absolutePath}"
        }
    }

    private fun importFolder() {
        val uri = importTreeUri ?: run { report("ابتدا پوشه ورودی را انتخاب کنید."); return }
        val repo = requireRepo() ?: return
        runGit("در حال واردکردن فایل‌ها...") {
            val source = DocumentFile.fromTreeUri(this, uri) ?: error("پوشه ورودی در دسترس نیست")
            copyTree(source, repo, repo)
            "فایل‌ها و پوشه‌ها وارد شدند. اکنون Commit + Push را اجرا کنید."
        }
    }

    private fun copyTree(source: DocumentFile, target: File, repo: File) {
        source.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            if (name == ".git") return@forEach
            val out = File(target, name)
            if (child.isDirectory) {
                out.mkdirs()
                copyTree(child, out, repo)
            } else if (child.isFile) {
                out.parentFile?.mkdirs()
                contentResolver.openInputStream(child.uri)?.use { input ->
                    FileOutputStream(out).use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun commitAndPush() {
        val repo = requireRepo() ?: return
        runGit("در حال Commit و Push...") {
            val git = Git.open(repo)
            git.add().addFilepattern(".").call()
            git.add().setUpdate(true).addFilepattern(".").call()
            val message = commitMessage.text.toString().trim().ifBlank { "Update from Easy Git" }
            val statusResult = git.status().call()
            if (statusResult.isClean) {
                git.close()
                return@runGit "تغییری برای Commit وجود ندارد."
            }
            git.commit().setMessage(message).setAuthor("Easy Git", "easy-git@users.noreply.github.com").call()
            git.push().setCredentialsProvider(credentials()).call()
            git.close()
            "Commit و Push با موفقیت انجام شد."
        }
    }

    private fun pull() {
        val repo = requireRepo() ?: return
        runGit("در حال دریافت تغییرات...") {
            val git = Git.open(repo)
            val result: PullResult = git.pull().setCredentialsProvider(credentials()).call()
            git.close()
            if (result.isSuccessful) "Pull با موفقیت انجام شد."
            else "Pull کامل نشد: ${result.mergeResult?.mergeStatus ?: "بررسی وضعیت لازم است"}"
        }
    }

    private fun showStatus() {
        val repo = requireRepo() ?: return
        runGit("در حال بررسی وضعیت...") {
            val git = Git.open(repo)
            val s = git.status().call()
            git.close()
            buildString {
                appendLine("Modified: ${s.modified.size}")
                appendLine("Added: ${s.added.size}")
                appendLine("Untracked: ${s.untracked.size}")
                appendLine("Removed: ${s.removed.size}")
                appendLine("Missing: ${s.missing.size}")
                appendLine("Conflicting: ${s.conflicting.size}")
                appendLine(if (s.isClean) "Working tree clean" else "تغییرات ثبت‌نشده وجود دارد")
            }
        }
    }

    private fun showHistory() {
        val repo = requireRepo() ?: return
        runGit("در حال خواندن تاریخچه...") {
            val git = Git.open(repo)
            val commits = git.log().setMaxCount(30).call().toList()
            git.close()
            if (commits.isEmpty()) "تاریخچه‌ای وجود ندارد."
            else commits.joinToString("\\n\\n") {
                "${it.name.take(8)}  ${it.authorIdent.name}\\n${it.shortMessage}"
            }
        }
    }

    private fun exportZip() {
        val repo = requireRepo() ?: return
        runGit("در حال ساخت ZIP...") {
            val dest = File(getExternalFilesDir(null) ?: filesDir, "exports/${repo.name}.zip")
            dest.parentFile?.mkdirs()
            java.util.zip.ZipOutputStream(FileOutputStream(dest)).use { zip ->
                repo.walkTopDown().filter { it.isFile && !it.toRelativeString(repo).startsWith(".git/") }
                    .forEach { file ->
                        val rel = file.relativeTo(repo).invariantSeparatorsPath
                        zip.putNextEntry(java.util.zip.ZipEntry(rel))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
            }
            "ZIP ذخیره شد: ${dest.absolutePath}"
        }
    }

    private fun credentials(): UsernamePasswordCredentialsProvider {
        val pat = token.text.toString().trim()
        if (pat.isBlank()) throw IllegalStateException("توکن GitHub را وارد کنید.")
        return UsernamePasswordCredentialsProvider("x-access-token", pat)
    }

    private fun requireRepo(): File? {
        val repo = repoDir
        if (repo == null || !File(repo, ".git").exists()) {
            report("ابتدا مخزن را با Clone دریافت کنید.")
            return null
        }
        return repo
    }

    private fun runGit(message: String, task: () -> String) {
        report(message)
        executor.execute {
            try {
                val result = task()
                runOnUiThread { report(result) }
            } catch (e: Exception) {
                runOnUiThread { report("خطا: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    private fun report(message: String) {
        status.text = message
        log.text = message
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

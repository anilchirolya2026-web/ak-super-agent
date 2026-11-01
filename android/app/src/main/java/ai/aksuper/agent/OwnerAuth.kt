package ai.aksuper.agent

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local owner lock for the Stage 2/3 prototype.
 *
 * - No password is stored in the APK. The owner sets it on first launch.
 * - Only a salted PBKDF2-SHA256 hash is kept in app-private storage (backup disabled).
 * - 5 wrong attempts lock the owner area for 5 minutes, doubling after each lockout.
 *
 * When the backend is live (Stage 3), owner login moves to the server with MFA,
 * and this lock stays only as an extra on-device app lock.
 */
object OwnerAuth {
    const val OWNER_EMAIL = "anil87569091@gmail.com"
    const val MIN_LENGTH = 10

    private const val PREFS = "owner_auth"
    private const val ITERATIONS = 120_000
    private const val MAX_FAILS = 5
    private const val BASE_LOCK_MS = 5 * 60 * 1000L

    sealed class Result {
        object Ok : Result()
        data class Wrong(val attemptsLeft: Int) : Result()
        data class Locked(val secondsLeft: Long) : Result()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isSetUp(ctx: Context): Boolean = prefs(ctx).contains("hash")

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun setPassword(ctx: Context, password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        prefs(ctx).edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt("fails", 0)
            .putInt("lockouts", 0)
            .putLong("locked_until", 0L)
            .apply()
    }

    fun lockedSecondsLeft(ctx: Context): Long {
        val until = prefs(ctx).getLong("locked_until", 0L)
        val left = until - System.currentTimeMillis()
        return if (left > 0) (left + 999) / 1000 else 0
    }

    fun verify(ctx: Context, email: String, password: String): Result {
        val locked = lockedSecondsLeft(ctx)
        if (locked > 0) return Result.Locked(locked)

        val p = prefs(ctx)
        val salt = Base64.decode(p.getString("salt", "") ?: "", Base64.NO_WRAP)
        val stored = Base64.decode(p.getString("hash", "") ?: "", Base64.NO_WRAP)
        val emailOk = email.trim().equals(OWNER_EMAIL, ignoreCase = true)
        // Always run the hash so timing does not reveal whether the email matched.
        val candidate = derive(password, salt)
        val passOk = MessageDigest.isEqual(candidate, stored)

        if (emailOk && passOk) {
            p.edit().putInt("fails", 0).putInt("lockouts", 0).apply()
            return Result.Ok
        }

        val fails = p.getInt("fails", 0) + 1
        if (fails >= MAX_FAILS) {
            val lockouts = p.getInt("lockouts", 0)
            val lockMs = BASE_LOCK_MS shl lockouts.coerceAtMost(5)
            p.edit()
                .putInt("fails", 0)
                .putInt("lockouts", lockouts + 1)
                .putLong("locked_until", System.currentTimeMillis() + lockMs)
                .apply()
            return Result.Locked(lockMs / 1000)
        }
        p.edit().putInt("fails", fails).apply()
        return Result.Wrong(MAX_FAILS - fails)
    }
}

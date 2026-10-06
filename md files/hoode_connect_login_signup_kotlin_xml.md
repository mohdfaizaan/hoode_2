# Hoode Connect — Login & Signup Screen Specification
### Android App — Kotlin + XML

This file defines the complete UI and implementation plan for the **Hoode Connect** authentication screens.

The design is based on the provided reference image, but customized for Hoode Connect:

- App name at the top: **Hoode Connect**
- No separate logo image required
- Remove the center onboarding screen
- Keep only **Login** and **Register / Sign Up**
- Remove Facebook login
- Keep only **Google Sign-In**
- Clean white + blue/purple gradient theme
- Rounded white authentication card
- Kotlin + XML Android implementation
- Suitable for Supabase Auth or another authentication backend

---

# 1. Screens Required

Only these two authentication screens are required:

1. **Login Screen**
2. **Register / Sign Up Screen**

Do **not** create the middle onboarding / "Get Started" screen from the reference.

---

# 2. Overall Design Style

Use the same visual structure as the reference:

- Top section with blue/purple gradient background
- Rounded bottom corners on the gradient area
- App title centered near the top
- Large white authentication card overlapping the gradient
- Card should have large rounded top corners
- White overall background
- Minimal, modern UI
- Thin light-gray input borders
- Large rounded gradient main button
- Google login button at the bottom
- No Facebook button

Recommended visual values:

| Element | Value |
|---|---|
| Main background | `#F8F9FF` |
| Primary blue | `#3438E8` |
| Secondary purple | `#8257E8` |
| Accent pink | `#E881F4` |
| Main text | `#20202A` |
| Secondary text | `#777781` |
| Input border | `#E6E6EC` |
| Card color | `#FFFFFF` |
| Button corner radius | `14dp` |
| Card top radius | `30dp` |
| Input height | `56dp` |

---

# 3. Login Screen

## Top Header

The top gradient area should contain:

- Back arrow on the left if needed
- Small text on the right:

`Don't have an account?`

- Small button:

`Get Started`

- Main app title in the center:

# Hoode Connect

Do not use the word **Jobsly**.

No separate app logo is necessary unless added later.

---

## Login Card

Main heading:

**Welcome Back**

Subheading:

`Enter your details below`

### Fields

1. Email Address
2. Password

Password field should include:

- hidden password by default
- eye icon to show / hide password

### Main Button

Text:

**Sign in**

Use a blue → purple → pink gradient.

### Forgot Password

Below the main button:

`Forgot your password?`

This should be clickable.

### Divider

Use:

`Or sign in with`

### Social Login

Show only:

**Continue with Google**

Do not show Facebook.

---

# 4. Register / Sign Up Screen

## Top Header

Small text:

`Already have an account?`

Button:

`Sign in`

Main title:

# Hoode Connect

---

## Register Card

Heading:

**Get started free.**

Subheading:

`Create your Hoode Connect account.`

### Fields

1. Email Address
2. Full Name
3. Password
4. Confirm Password

Password field can optionally show a small strength indicator.

### Main Button

Text:

**Sign up**

Use the same gradient style as Login.

### Divider

`Or sign up with`

### Social Sign-In

Only:

**Continue with Google**

Do not include Facebook.

---

# 5. Suggested Android File Structure

```text
app/
└── src/
    └── main/
        ├── java/com/hoode/connect/
        │   ├── auth/
        │   │   ├── LoginActivity.kt
        │   │   ├── RegisterActivity.kt
        │   │   └── AuthManager.kt
        │   └── MainActivity.kt
        │
        └── res/
            ├── layout/
            │   ├── activity_login.xml
            │   └── activity_register.xml
            │
            ├── drawable/
            │   ├── bg_auth_gradient.xml
            │   ├── bg_auth_card.xml
            │   ├── bg_input.xml
            │   ├── bg_primary_button.xml
            │   └── bg_google_button.xml
            │
            ├── values/
            │   ├── colors.xml
            │   ├── dimens.xml
            │   ├── strings.xml
            │   └── themes.xml
            │
            └── mipmap/
```

---

# 6. Colors

## `res/values/colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <color name="auth_blue">#3438E8</color>
    <color name="auth_purple">#7557E8</color>
    <color name="auth_pink">#E780F4</color>

    <color name="auth_background">#F8F9FF</color>
    <color name="auth_card">#FFFFFF</color>

    <color name="text_primary">#20202A</color>
    <color name="text_secondary">#777781</color>

    <color name="input_border">#E7E7ED</color>
    <color name="divider_color">#E6E6EB</color>

    <color name="white">#FFFFFF</color>
    <color name="black">#000000</color>

</resources>
```

---

# 7. Header Gradient

## `res/drawable/bg_auth_gradient.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">

    <gradient
        android:angle="0"
        android:startColor="#3438E8"
        android:centerColor="#5548E6"
        android:endColor="#8270E8" />

    <corners
        android:bottomLeftRadius="34dp"
        android:bottomRightRadius="34dp" />

</shape>
```

---

# 8. White Authentication Card

## `res/drawable/bg_auth_card.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">

    <solid android:color="#FFFFFF" />

    <corners
        android:topLeftRadius="30dp"
        android:topRightRadius="30dp" />

</shape>
```

---

# 9. Input Background

## `res/drawable/bg_input.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">

    <solid android:color="#FFFFFF" />

    <stroke
        android:width="1dp"
        android:color="#E7E7ED" />

    <corners android:radius="12dp" />

</shape>
```

---

# 10. Gradient Main Button

## `res/drawable/bg_primary_button.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">

    <gradient
        android:angle="0"
        android:startColor="#3438E8"
        android:centerColor="#7557E8"
        android:endColor="#E780F4" />

    <corners android:radius="14dp" />

</shape>
```

---

# 11. Google Button Background

## `res/drawable/bg_google_button.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">

    <solid android:color="#FFFFFF" />

    <stroke
        android:width="1dp"
        android:color="#E7E7ED" />

    <corners android:radius="12dp" />

</shape>
```

---

# 12. Login Layout

## `res/layout/activity_login.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>

<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/auth_background">

    <!-- TOP GRADIENT -->

    <LinearLayout
        android:id="@+id/topHeader"
        android:layout_width="match_parent"
        android:layout_height="290dp"
        android:background="@drawable/bg_auth_gradient"
        android:orientation="vertical"
        android:paddingStart="24dp"
        android:paddingTop="42dp"
        android:paddingEnd="24dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <ImageButton
                android:id="@+id/btnBack"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="@android:color/transparent"
                android:contentDescription="Back"
                android:src="@drawable/ic_arrow_back"
                app:tint="@color/white" />

            <Space
                android:layout_width="0dp"
                android:layout_height="1dp"
                android:layout_weight="1" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Don't have an account?"
                android:textColor="#E6E6FF"
                android:textSize="12sp" />

            <TextView
                android:id="@+id/tvGetStarted"
                android:layout_width="wrap_content"
                android:layout_height="36dp"
                android:layout_marginStart="8dp"
                android:background="@drawable/bg_small_header_button"
                android:gravity="center"
                android:paddingStart="12dp"
                android:paddingEnd="12dp"
                android:text="Get Started"
                android:textColor="@color/white"
                android:textSize="12sp"
                android:textStyle="bold" />

        </LinearLayout>

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="34dp"
            android:gravity="center"
            android:text="Hoode Connect"
            android:textColor="@color/white"
            android:textSize="31sp"
            android:textStyle="bold" />

    </LinearLayout>

    <!-- WHITE AUTH CARD -->

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginTop="225dp"
        android:background="@drawable/bg_auth_card"
        android:fillViewport="true"
        android:overScrollMode="never">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingStart="24dp"
            android:paddingTop="34dp"
            android:paddingEnd="24dp"
            android:paddingBottom="28dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Welcome Back"
                android:textColor="@color/text_primary"
                android:textSize="26sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:text="Enter your details below"
                android:textColor="@color/text_secondary"
                android:textSize="14sp" />

            <EditText
                android:id="@+id/etEmail"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:layout_marginTop="28dp"
                android:background="@drawable/bg_input"
                android:hint="Email Address"
                android:inputType="textEmailAddress"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:textColor="@color/text_primary"
                android:textColorHint="@color/text_secondary" />

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/passwordLayout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                app:boxBackgroundColor="@color/white"
                app:boxBackgroundMode="outline"
                app:boxCornerRadiusBottomEnd="12dp"
                app:boxCornerRadiusBottomStart="12dp"
                app:boxCornerRadiusTopEnd="12dp"
                app:boxCornerRadiusTopStart="12dp"
                app:boxStrokeColor="@color/input_border"
                app:endIconMode="password_toggle"
                app:hintEnabled="false">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etPassword"
                    android:layout_width="match_parent"
                    android:layout_height="56dp"
                    android:hint="Password"
                    android:inputType="textPassword"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp" />

            </com.google.android.material.textfield.TextInputLayout>

            <TextView
                android:id="@+id/btnSignIn"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:layout_marginTop="20dp"
                android:background="@drawable/bg_primary_button"
                android:gravity="center"
                android:text="Sign in"
                android:textColor="@color/white"
                android:textSize="15sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvForgotPassword"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="18dp"
                android:gravity="center"
                android:text="Forgot your password?"
                android:textColor="@color/text_primary"
                android:textSize="14sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="28dp"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <View
                    android:layout_width="0dp"
                    android:layout_height="1dp"
                    android:layout_weight="1"
                    android:background="@color/divider_color" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="14dp"
                    android:layout_marginEnd="14dp"
                    android:text="Or sign in with"
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />

                <View
                    android:layout_width="0dp"
                    android:layout_height="1dp"
                    android:layout_weight="1"
                    android:background="@color/divider_color" />

            </LinearLayout>

            <LinearLayout
                android:id="@+id/btnGoogle"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:layout_marginTop="20dp"
                android:background="@drawable/bg_google_button"
                android:gravity="center"
                android:orientation="horizontal">

                <ImageView
                    android:layout_width="22dp"
                    android:layout_height="22dp"
                    android:contentDescription="Google"
                    android:src="@drawable/ic_google" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="12dp"
                    android:text="Continue with Google"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp"
                    android:textStyle="bold" />

            </LinearLayout>

        </LinearLayout>

    </ScrollView>

</FrameLayout>
```

---

# 13. Register Layout

## `res/layout/activity_register.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>

<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/auth_background">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="290dp"
        android:background="@drawable/bg_auth_gradient"
        android:orientation="vertical"
        android:paddingStart="24dp"
        android:paddingTop="42dp"
        android:paddingEnd="24dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <ImageButton
                android:id="@+id/btnBack"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="@android:color/transparent"
                android:contentDescription="Back"
                android:src="@drawable/ic_arrow_back"
                app:tint="@color/white" />

            <Space
                android:layout_width="0dp"
                android:layout_height="1dp"
                android:layout_weight="1" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Already have an account?"
                android:textColor="#E6E6FF"
                android:textSize="12sp" />

            <TextView
                android:id="@+id/tvSignIn"
                android:layout_width="wrap_content"
                android:layout_height="36dp"
                android:layout_marginStart="8dp"
                android:background="@drawable/bg_small_header_button"
                android:gravity="center"
                android:paddingStart="14dp"
                android:paddingEnd="14dp"
                android:text="Sign in"
                android:textColor="@color/white"
                android:textSize="12sp"
                android:textStyle="bold" />

        </LinearLayout>

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="34dp"
            android:gravity="center"
            android:text="Hoode Connect"
            android:textColor="@color/white"
            android:textSize="31sp"
            android:textStyle="bold" />

    </LinearLayout>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginTop="225dp"
        android:background="@drawable/bg_auth_card"
        android:fillViewport="true"
        android:overScrollMode="never">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingStart="24dp"
            android:paddingTop="34dp"
            android:paddingEnd="24dp"
            android:paddingBottom="28dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Get started free."
                android:textColor="@color/text_primary"
                android:textSize="26sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:text="Create your Hoode Connect account."
                android:textColor="@color/text_secondary"
                android:textSize="14sp" />

            <EditText
                android:id="@+id/etEmail"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:layout_marginTop="26dp"
                android:background="@drawable/bg_input"
                android:hint="Email Address"
                android:inputType="textEmailAddress"
                android:paddingStart="16dp"
                android:paddingEnd="16dp" />

            <EditText
                android:id="@+id/etName"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:layout_marginTop="14dp"
                android:background="@drawable/bg_input"
                android:hint="Full Name"
                android:inputType="textPersonName"
                android:paddingStart="16dp"
                android:paddingEnd="16dp" />

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                app:boxBackgroundMode="outline"
                app:boxCornerRadiusBottomEnd="12dp"
                app:boxCornerRadiusBottomStart="12dp"
                app:boxCornerRadiusTopEnd="12dp"
                app:boxCornerRadiusTopStart="12dp"
                app:endIconMode="password_toggle"
                app:hintEnabled="false">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etPassword"
                    android:layout_width="match_parent"
                    android:layout_height="56dp"
                    android:hint="Password"
                    android:inputType="textPassword" />

            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                app:boxBackgroundMode="outline"
                app:boxCornerRadiusBottomEnd="12dp"
                app:boxCornerRadiusBottomStart="12dp"
                app:boxCornerRadiusTopEnd="12dp"
                app:boxCornerRadiusTopStart="12dp"
                app:endIconMode="password_toggle"
                app:hintEnabled="false">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etConfirmPassword"
                    android:layout_width="match_parent"
                    android:layout_height="56dp"
                    android:hint="Confirm Password"
                    android:inputType="textPassword" />

            </com.google.android.material.textfield.TextInputLayout>

            <TextView
                android:id="@+id/btnSignUp"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:layout_marginTop="20dp"
                android:background="@drawable/bg_primary_button"
                android:gravity="center"
                android:text="Sign up"
                android:textColor="@color/white"
                android:textSize="15sp"
                android:textStyle="bold" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="28dp"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <View
                    android:layout_width="0dp"
                    android:layout_height="1dp"
                    android:layout_weight="1"
                    android:background="@color/divider_color" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="14dp"
                    android:layout_marginEnd="14dp"
                    android:text="Or sign up with"
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />

                <View
                    android:layout_width="0dp"
                    android:layout_height="1dp"
                    android:layout_weight="1"
                    android:background="@color/divider_color" />

            </LinearLayout>

            <LinearLayout
                android:id="@+id/btnGoogle"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:layout_marginTop="20dp"
                android:background="@drawable/bg_google_button"
                android:gravity="center"
                android:orientation="horizontal">

                <ImageView
                    android:layout_width="22dp"
                    android:layout_height="22dp"
                    android:src="@drawable/ic_google" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="12dp"
                    android:text="Continue with Google"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp"
                    android:textStyle="bold" />

            </LinearLayout>

        </LinearLayout>

    </ScrollView>

</FrameLayout>
```

---

# 14. Small Header Button Background

Create:

## `res/drawable/bg_small_header_button.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape
    xmlns:android="http://schemas.android.com/apk/res/android">

    <solid android:color="#33FFFFFF" />

    <corners android:radius="9dp" />

</shape>
```

---

# 15. Login Activity — Kotlin

## `LoginActivity.kt`

```kotlin
package com.hoode.connect.auth

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hoode.connect.MainActivity
import com.hoode.connect.R
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: android.widget.EditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var signInButton: TextView
    private lateinit var googleButton: LinearLayout
    private lateinit var getStartedButton: TextView
    private lateinit var forgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        signInButton = findViewById(R.id.btnSignIn)
        googleButton = findViewById(R.id.btnGoogle)
        getStartedButton = findViewById(R.id.tvGetStarted)
        forgotPassword = findViewById(R.id.tvForgotPassword)

        signInButton.setOnClickListener {
            loginWithEmail()
        }

        googleButton.setOnClickListener {
            signInWithGoogle()
        }

        getStartedButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }

        forgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                "Password reset",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loginWithEmail() {

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty()) {
            emailInput.error = "Enter your email"
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Enter your password"
            return
        }

        /*
         * Replace this section with Supabase Auth.
         *
         * Example logic:
         *
         * supabase.auth.signInWith(Email) {
         *     this.email = email
         *     this.password = password
         * }
         */

        openMainScreen()
    }

    private fun signInWithGoogle() {

        /*
         * Connect this button with Google OAuth.
         *
         * Recommended:
         * Google Identity / Credential Manager
         * +
         * Supabase Google OAuth authentication.
         */

    }

    private fun openMainScreen() {

        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )

        finish()
    }
}
```

---

# 16. Register Activity — Kotlin

## `RegisterActivity.kt`

```kotlin
package com.hoode.connect.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hoode.connect.R
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText

    private lateinit var signUpButton: TextView
    private lateinit var googleButton: LinearLayout
    private lateinit var signInButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        emailInput = findViewById(R.id.etEmail)
        nameInput = findViewById(R.id.etName)
        passwordInput = findViewById(R.id.etPassword)
        confirmPasswordInput = findViewById(R.id.etConfirmPassword)

        signUpButton = findViewById(R.id.btnSignUp)
        googleButton = findViewById(R.id.btnGoogle)
        signInButton = findViewById(R.id.tvSignIn)

        signUpButton.setOnClickListener {
            registerUser()
        }

        googleButton.setOnClickListener {
            signUpWithGoogle()
        }

        signInButton.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {

        val email = emailInput.text.toString().trim()
        val name = nameInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        if (name.isEmpty()) {
            nameInput.error = "Enter your full name"
            return
        }

        if (email.isEmpty()) {
            emailInput.error = "Enter your email"
            return
        }

        if (password.length < 6) {
            passwordInput.error =
                "Password must contain at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            confirmPasswordInput.error =
                "Passwords do not match"
            return
        }

        /*
         * Replace with Supabase registration.
         *
         * Example:
         *
         * supabase.auth.signUpWith(Email) {
         *     this.email = email
         *     this.password = password
         * }
         *
         * Save "name" in your users/profile table.
         */

        Toast.makeText(
            this,
            "Account created",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun signUpWithGoogle() {

        /*
         * Google OAuth / Supabase Google sign-in
         * implementation goes here.
         */

    }
}
```

---

# 17. Recommended Authentication Flow

```text
App Launch
    ↓
Check current auth session
    ↓
┌──────────────────────┐
│ User already logged in? │
└──────────────────────┘
       ↓ YES              ↓ NO
MainActivity          LoginActivity
                          ↓
               ┌──────────┴──────────┐
               ↓                     ↓
          Email Login           Google Login
               ↓                     ↓
          MainActivity          MainActivity

LoginActivity
      ↓
"Get Started"
      ↓
RegisterActivity
      ↓
 ┌────┴────┐
 ↓         ↓
Email     Google
Sign Up   Sign Up
 ↓         ↓
MainActivity
```

---

# 18. Supabase Recommendation

For Hoode Connect, the authentication system can use:

### Supabase Auth

Support:

- Email + password registration
- Email + password login
- Google OAuth
- Session management
- Password reset
- Email verification if required

Suggested user profile table:

```text
profiles
--------------------------------
id
full_name
email
profile_image_url
created_at
role
```

Example roles:

```text
user
admin
moderator
```

The authentication UI should stay separate from application permission logic.

---

# 19. Google Sign-In

Only Google should appear as social login.

Do not include:

- Facebook
- Apple
- X / Twitter
- GitHub

Google button text:

**Continue with Google**

Recommended Google icon:

```text
ic_google.xml
```

or use an official Google "G" asset.

---

# 20. Responsive Design

The screen must work correctly on:

- small Android phones
- normal Android phones
- large Android phones

Use:

- `ScrollView`
- `match_parent`
- DP/SP units
- avoid fixed widths
- keep horizontal margins around `24dp`

Do not make the authentication card excessively tall.

The UI should remain vertically scrollable when the keyboard is open.

Recommended manifest setting:

```xml
<activity
    android:name=".auth.LoginActivity"
    android:windowSoftInputMode="adjustResize" />

<activity
    android:name=".auth.RegisterActivity"
    android:windowSoftInputMode="adjustResize" />
```

---

# 21. Status Bar

For the login/register screen:

```kotlin
window.statusBarColor =
    android.graphics.Color.TRANSPARENT
```

Use white status-bar icons when the gradient is visible behind them.

---

# 22. Navigation Rules

### Login → Register

When user taps:

`Get Started`

Open:

```text
RegisterActivity
```

### Register → Login

When user taps:

`Sign in`

Return to:

```text
LoginActivity
```

### Successful Login

Open:

```text
MainActivity
```

### Successful Registration

Recommended flow:

```text
Register
↓
Account created
↓
Session created
↓
MainActivity
```

If email verification is enabled:

```text
Register
↓
Verification email
↓
Verification Screen
↓
Login
```

---

# 23. Important UI Rules

The final Hoode Connect authentication screen should follow these rules:

- Use **Hoode Connect** instead of Jobsly
- No center onboarding screen
- Only Login + Register screens
- No Facebook button
- Google login only
- White rounded card
- Blue/purple gradient top area
- Modern minimal design
- Password visibility toggle
- Forgot Password option
- Full Name only on Sign Up
- Email + Password on Login
- Email + Full Name + Password + Confirm Password on Sign Up
- Avoid unnecessary visual clutter
- Keep typography clean and premium

---

# 24. Final Visual Structure

## Login

```text
╭─────────────────────────────────────╮
│ ←       Don't have an account?      │
│                       Get Started   │
│                                     │
│            Hoode Connect            │
│                                     │
╰───────────────╮   ╭─────────────────╯
                │
╭─────────────────────────────────────╮
│ Welcome Back                        │
│ Enter your details below            │
│                                     │
│ [ Email Address                  ]   │
│ [ Password                      👁 ] │
│                                     │
│ [          SIGN IN              ]   │
│                                     │
│      Forgot your password?          │
│                                     │
│ ─────── Or sign in with ───────    │
│                                     │
│ [ G    Continue with Google     ]   │
╰─────────────────────────────────────╯
```

---

## Register

```text
╭─────────────────────────────────────╮
│ ←       Already have an account?    │
│                            Sign in   │
│                                     │
│            Hoode Connect            │
│                                     │
╰───────────────╮   ╭─────────────────╯
                │
╭─────────────────────────────────────╮
│ Get started free.                   │
│ Create your Hoode Connect account.  │
│                                     │
│ [ Email Address                  ]   │
│ [ Full Name                      ]   │
│ [ Password                      👁 ] │
│ [ Confirm Password              👁 ] │
│                                     │
│ [          SIGN UP              ]   │
│                                     │
│ ─────── Or sign up with ───────    │
│                                     │
│ [ G    Continue with Google     ]   │
╰─────────────────────────────────────╯
```

---

# 25. Final Development Goal

The finished authentication area should feel similar to the reference design while still being clearly branded as **Hoode Connect**.

The design should:

- feel clean
- feel premium
- have smooth rounded shapes
- use the Hoode Connect blue/purple gradient
- keep registration simple
- make Google login prominent
- avoid unnecessary screens

Only the following authentication screens are required:

```text
Login
Register / Sign Up
```

The reference image's center onboarding screen must **not** be implemented.

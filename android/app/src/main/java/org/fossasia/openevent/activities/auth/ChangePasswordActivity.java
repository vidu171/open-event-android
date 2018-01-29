package org.fossasia.openevent.activities.auth;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.fossasia.openevent.R;
import org.fossasia.openevent.data.auth.User;
import org.fossasia.openevent.utils.Utils;
import org.fossasia.openevent.viewmodels.auth.ChangePasswordActivityViewModel;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChangePasswordActivity extends AppCompatActivity {

    @BindView(R.id.text_input_layout_current_password)
    TextInputLayout currentPasswordWrapper;
    @BindView(R.id.text_input_layout_new_password)
    TextInputLayout newPasswordWrapper;
    @BindView(R.id.text_input_layout_confirm_password)
    TextInputLayout confirmPasswordWrapper;
    @BindView(R.id.btnChangePassword)
    Button changePassword;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    String currentPassword;
    String newPassword;

    private AppCompatEditText currentPasswordInput;
    private AppCompatEditText newPasswordInput;
    private AppCompatEditText confirmPasswordInput;

    private User user;
    private ChangePasswordActivityViewModel changePasswordActivityViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        changePasswordActivityViewModel = ViewModelProviders.of(this).get(ChangePasswordActivityViewModel.class);
        changePasswordActivityViewModel.getUser().observe(this, user -> {
            this.user = user;
        });

        currentPasswordInput = (AppCompatEditText) currentPasswordWrapper.getEditText();
        newPasswordInput = (AppCompatEditText) newPasswordWrapper.getEditText();
        confirmPasswordInput = (AppCompatEditText) confirmPasswordWrapper.getEditText();

        changePassword.setOnClickListener(v -> {
            hideKeyBoard();
            saveChanges();
        });
    }

    private void saveChanges() {
        currentPassword = currentPasswordInput.getText().toString();
        newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (validateCredentials(currentPassword, newPassword, confirmPassword)) {
            checkCurrentPassword(currentPassword);
        }
    }

    private void checkCurrentPassword(String currentPassword) {
        if (user == null)
            return;

        showProgressBar(true);
        changePasswordActivityViewModel.checkCurrentPassword(currentPassword).observe(this, response -> {
            switch (response) {
                case ChangePasswordActivityViewModel.ON_ERROR:
                    Toast.makeText(ChangePasswordActivity.this, R.string.error_password_not_correct, Toast.LENGTH_SHORT).show();
                    break;
                case ChangePasswordActivityViewModel.ON_COMPLETE:
                    //Old password matched change
                    changePassword();
                    break;
                default:
                    //Not implemented
            }
            showProgressBar(false);
        });
    }

    private void changePassword() {
        changePasswordActivityViewModel.changePassword(newPassword).observe(this, response -> {
            switch (response) {
                case ChangePasswordActivityViewModel.ON_ERROR:
                    Toast.makeText(ChangePasswordActivity.this, R.string.error_changing_password, Toast.LENGTH_SHORT).show();
                    break;
                case ChangePasswordActivityViewModel.ON_COMPLETE:
                    Toast.makeText(ChangePasswordActivity.this, R.string.password_changed_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case ChangePasswordActivityViewModel.ON_EMPTY_PASSWORD:
                    Toast.makeText(ChangePasswordActivity.this, R.string.error_password_empty, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    //Not implemented
            }
        });
    }

    private boolean validateCredentials(String currentPassword, String newPassword, String confirmPasssword) {

        // Reset errors.
        currentPasswordWrapper.setError(null);
        newPasswordWrapper.setError(null);
        confirmPasswordWrapper.setError(null);

        if (Utils.isEmpty(currentPassword)) {
            handleError(currentPasswordWrapper, R.string.error_password_required);
            return false;
        }

        if (Utils.isEmpty(newPassword)) {
            handleError(newPasswordWrapper, R.string.error_password_required);
            return false;
        } else if (!Utils.isPasswordValid(newPassword)) {
            handleError(newPasswordWrapper, R.string.error_password_length);
            return false;
        }

        if (Utils.isEmpty(confirmPasssword)) {
            handleError(confirmPasswordWrapper, R.string.error_confirm_password);
            return false;
        }

        if (!confirmPasssword.equals(newPassword)) {
            handleError(confirmPasswordWrapper, R.string.error_password_not_matching);
            return false;
        }

        return true;
    }

    private void handleError(TextInputLayout textInputLayout, @StringRes int id) {
        textInputLayout.setError(getString(id));
        textInputLayout.requestFocus();
    }

    private void showProgressBar(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void hideKeyBoard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            view.clearFocus();
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                //do nothing
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

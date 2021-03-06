package com.thecoolguy.rumaan.fileio.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.crashlytics.android.Crashlytics;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.github.kittinunf.fuel.core.FuelError;
import com.thecoolguy.rumaan.fileio.R;
import com.thecoolguy.rumaan.fileio.data.Upload;
import com.thecoolguy.rumaan.fileio.data.UploadItemViewModel;
import com.thecoolguy.rumaan.fileio.data.models.FileModel;
import com.thecoolguy.rumaan.fileio.utils.Consts;
import com.thecoolguy.rumaan.fileio.utils.FileUtils;
import com.thecoolguy.rumaan.fileio.utils.MaterialIn;
import java.io.File;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements
    DialogClickListener, Upload {

  public static final String TAG = "MainActivity";

  public static final int INTENT_FILE_REQUEST = 42;
  Animator animator;
  @BindView(R.id.uploading_text)
  TextView uploadingText;
  @BindView(R.id.upload_progress)
  NumberProgressBar progressBar;
  UploadItemViewModel uploadItemViewModel;
  @BindView(R.id.link)
  TextView linkTextView;
  @BindView(R.id.root_view)
  ConstraintLayout rootView;
  @BindView(R.id.toolbar)
  Toolbar toolbar;


  @OnClick(R.id.btn_upload)
  void onClickUploadButton() {
    MainActivityPermissionsDispatcher.chooseFileWithPermissionCheck(MainActivity.this, null);
  }

  @OnClick(R.id.copy)
  void onCopyClick() {
    // Copy the content of the link text to Clipboard
    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    ClipData clipData = ClipData.newPlainText("link", linkTextView.getText());
    if (clipboardManager != null) {
      clipboardManager.setPrimaryClip(clipData);
      Toast.makeText(MainActivity.this, getString(R.string.link_copy), Toast.LENGTH_SHORT).show();
    }
  }

  void showHistory() {
    startActivity(new Intent(this, UploadHistoryActivity.class));
  }

  void showAbout() {
    startActivity(new Intent(this, AboutActivity.class));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == INTENT_FILE_REQUEST) {
      if (resultCode == RESULT_OK) {
        Uri fileUri = data.getData();
        if (fileUri != null) {
          Log.d(TAG, "onActivityResult: " + fileUri.toString());
          handleFileUri(fileUri);
        } else {
          Log.e(TAG, "onActivityResult: ERROR", new NullPointerException("File path URI is null"));
          Toast.makeText(
              this, "Some Error Occurred.", Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(this, getString(R.string.cancel_file_choose_msg), Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void showExpireDaysFragment() {
    final ChooseExpireDaysFragment chooseExpireDaysFragment = new ChooseExpireDaysFragment();
    chooseExpireDaysFragment.show(getSupportFragmentManager(), "choose_expire_days");
  }

  /**
   * @param uri Uri of the file chosen. Handles the file uri and passes onto the upload process
   */
  private void handleFileUri(Uri uri) {
    //  get the actual file from the storage
    final File file = FileUtils.getFile(this, uri);

    if (file != null) {
      uploadItemViewModel.setFileModel(new FileModel(file, Consts.DEFAULT_EXPIRE_WEEKS + "w"));
      showExpireDaysFragment();
    }
  }

  public void showUploadingView(boolean show) {
    final RelativeLayout uploadLayoutView = findViewById(R.id.root_view_upload);

    View view = findViewById(R.id.btn_upload);
    // Mask view animations
    // Get the view center
    int cx = (view.getLeft() + view.getRight()) / 2;
    int cy = (view.getTop() + view.getBottom()) / 2;
    int startRadius = view.getHeight() / 2;
    float finalRadius = (float) Math
        .hypot(uploadLayoutView.getWidth(), uploadLayoutView.getHeight());

    if (show) {
      animator = ViewAnimationUtils
          .createCircularReveal(uploadLayoutView, cx, cy, startRadius, finalRadius);
      animator.setDuration(700);
      animator.setInterpolator(new FastOutSlowInInterpolator());

      uploadLayoutView.setVisibility(View.VISIBLE);

      animator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          super.onAnimationEnd(animation);
          uploadingText.setVisibility(View.VISIBLE);
          progressBar.setVisibility(View.VISIBLE);
          MaterialIn.animate(uploadingText, Gravity.TOP, Gravity.TOP);
          MaterialIn.animate(progressBar);
        }
      });

      animator.start();

    } else {
      animator = ViewAnimationUtils
          .createCircularReveal(uploadLayoutView, cx, cy, finalRadius, startRadius);
      animator.setDuration(700);
      animator.setInterpolator(new FastOutSlowInInterpolator());

      animator.addListener(new AnimatorListenerAdapter() {

        @Override
        public void onAnimationStart(Animator animation) {
          super.onAnimationStart(animation);
          uploadingText.setVisibility(View.INVISIBLE);
          progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
          super.onAnimationEnd(animation);

          uploadLayoutView.setVisibility(View.INVISIBLE);

        }
      });

      animator.start();
    }

    animator = null;
  }

  @NeedsPermission({Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE})
  public void uploadFile() {
    // Show progress dialog
    showUploadingView(true);
    uploadItemViewModel.uploadFile(this);
  }

  void updateLinkText(String link) {
    linkTextView.setText(link);

    Transition transition = new AutoTransition()
        .setDuration(500)
        .setStartDelay(300)
        .setInterpolator(new AccelerateDecelerateInterpolator());
    TransitionManager.beginDelayedTransition(rootView, transition);

    linkTextView.setVisibility(View.VISIBLE);
  }

  @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE})
  public void chooseFile(Intent dataIntent) {
        /* Check for network connectivity */
    if (isConnectedToActiveNetwork(this)) {
      // if upload was chosen from the app
      if (dataIntent == null) {
        // Use system file browser
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Choose the file to Upload.."),
            INTENT_FILE_REQUEST);

      } else {
        // If Intent received from implicit intent
        onActivityResult(INTENT_FILE_REQUEST, RESULT_OK, dataIntent);
      }
    } else {
            /* Show no network dialog */
      NoNetworkDialogFragment noNetworkDialogFragment = new NoNetworkDialogFragment();
      noNetworkDialogFragment
          .show(getSupportFragmentManager(), getString(R.string.no_net_dialog_fragment_tag));
    }

  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.options_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_history:
        showHistory();
        return true;
      case R.id.menu_about:
        showAbout();
        return true;
    }
    return true;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

        /* This neat lil trick though! */
    setTheme(R.style.NoActionBarTheme);

    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

        /* Get the view model */
    uploadItemViewModel = ViewModelProviders.of(this).get(UploadItemViewModel.class);

        /* Handle incoming intent content */
    Intent intent = getIntent();
    String action = intent.getAction();
    String type = intent.getType();
    // FIXME: Google photos URI
    if (type != null) {
      Log.d(TAG, "Receive Type: " + type);
      Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
      Log.d(TAG, "\nURI: " + fileUri);
      if (Intent.ACTION_SEND.equals(action) && fileUri != null) {
        handleExplicitFileShare(intent);
      }
    }

  }

  /* Handle incoming intent from file share apps */
  private void handleExplicitFileShare(Intent intent) {
    MainActivityPermissionsDispatcher.chooseFileWithPermissionCheck(this, intent);
  }

  @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE})
  void showPermissionDeniedForStorage() {
    Toast.makeText(this, getString(R.string.permission_deny), Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
  }

  /* Opens App info screen in settings */
  @OnNeverAskAgain({Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE})
  void showAppDetailsSettings() {
    Toast.makeText(this, getString(R.string.app_wont_work), Toast.LENGTH_LONG).show();
    try {
      Intent intent;
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        intent = new Intent(Intent.ACTION_APPLICATION_PREFERENCES);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
      } else {
        intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package: " + getPackageName()));
        startActivity(intent);
      }
    } catch (ActivityNotFoundException e) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
      startActivity(intent);
      e.printStackTrace();
    }
  }

  /* Check for current network state */
  @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
  boolean isConnectedToActiveNetwork(Context context) {
    ConnectivityManager connectivityManager = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo;
    if (connectivityManager != null) {
      networkInfo = connectivityManager.getActiveNetworkInfo();
      return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
    return false;
  }


  @Override
  public void onUpload(String result) {
    showUploadingView(false);
    Log.d(TAG, "onUpload: " + result);
    // update the text view
    updateLinkText(result);
  }

  @Override
  public void progress(final int progress) {
    // uploading progress on the UI
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        progressBar.setProgress(progress);
      }
    });
  }

  @Override
  public void onError(FuelError error) {
    error.getException().printStackTrace();
    Crashlytics.logException(error.getException());
  }

  @Override
  public void onDelete() {
    // on upload item delete
  }

  private void dismissDialog(Dialog dialog) {
    if (dialog == null) {
      return;
    }

    if (dialog.isShowing()) {
      dialog.dismiss();
    }
  }

  @Override
  public void onDialogPositiveClick(Dialog dialog, Fragment fragment) {
    if (fragment instanceof NoNetworkDialogFragment) {
      dismissDialog(dialog);
    }

    if (fragment instanceof ChooseExpireDaysFragment) {
      int value = ((ChooseExpireDaysFragment) fragment).getNumberPickerValue();
      Log.d(TAG, "Number Picker Value: " + value);

      dismissDialog(dialog);

      // update the selected days to expire in the view model
      uploadItemViewModel.getFileModel().setDaysToExpire(value + "w");

      // upload the file
      MainActivityPermissionsDispatcher.uploadFileWithPermissionCheck(this);
    }
  }
}

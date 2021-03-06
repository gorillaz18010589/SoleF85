package com.dyaco.spiritbike;

import androidx.navigation.Navigation;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DebugUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.corestar.libs.device.Device;
import com.dyaco.spiritbike.engineer.DeviceSettingBean;
import com.dyaco.spiritbike.product_flavors.ModeEnum;
import com.dyaco.spiritbike.settings.UpdateBean;
import com.dyaco.spiritbike.settings.UpdateSoftwareActivity;
import com.dyaco.spiritbike.support.BaseAppCompatActivity;
import com.dyaco.spiritbike.support.BtnExitFullScreen;
import com.dyaco.spiritbike.support.CommonUtils;
import com.dyaco.spiritbike.support.LogS;
import com.dyaco.spiritbike.support.MsgEvent;
import com.dyaco.spiritbike.support.RxBus;
import com.dyaco.spiritbike.support.RxTimer;
import com.dyaco.spiritbike.support.TimeZoneBean;
import com.dyaco.spiritbike.support.room.DatabaseManager;
import com.dyaco.spiritbike.webapi.BaseApi;
import com.dyaco.spiritbike.webapi.IServiceApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tencent.mmkv.MMKV;

import java.util.TimeZone;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

import static com.dyaco.spiritbike.MyApplication.MODE;
import static com.dyaco.spiritbike.MyApplication.ON_DEVICE_INFO;
import static com.dyaco.spiritbike.MyApplication.ON_ERROR;

import static com.dyaco.spiritbike.MyApplication.ON_LWR_MODE;
import static com.dyaco.spiritbike.MyApplication.btnFnaI;
import static com.dyaco.spiritbike.MyApplication.getInstance;
import static com.dyaco.spiritbike.MyApplication.isBootUp;
import static com.dyaco.spiritbike.MyApplication.isUartConPortOpen;
import static com.dyaco.spiritbike.MyApplication.updateNotify;
import static com.dyaco.spiritbike.support.CommonUtils.checkSwVersion;
import static com.dyaco.spiritbike.support.CommonUtils.convertSwVersion;

public class MainActivity extends BaseAppCompatActivity {
    DeviceSettingBean deviceSettingBean = getInstance().getDeviceSettingBean();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private boolean isForce;
    private int fragment_id;

    private RxTimer checkUpdateRxTimer;

    private BtnExitFullScreen btnExitFullScreen = new BtnExitFullScreen(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Log.d("SYSTEM_APP", "onCreate: " + ApplicationInfo.FLAG_SYSTEM); // 1 false

        //   Log.d("MMMMMMM", "onCreate: " + CommonUtils.getMacAddress());
// 90:09:17:1B:84:71
//        if (!CommonUtils.getMacAddress().contains("90:09:17:")) {
//            isSupported = false;
//            return;
//        }


        btnFnaI = 0;
        getInstance().mDevice.setFan(Device.FAN.STOP);

        initEvent();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        fragment_id = bundle != null ? bundle.getInt("fragment_id", 0) : 0;

        //getIdentity()???????????????????????????GUID
        if (TextUtils.isEmpty(getInstance().getIdentity())) {
            //  MMKV.defaultMMKV().encode("GUID", java.util.UUID.randomUUID().toString() + "#XE395ENT");
            MMKV.defaultMMKV().encode("GUID", java.util.UUID.randomUUID().toString() + "#" + deviceSettingBean.getModel_name());
        }

        if (!getPackageManager().canRequestPackageInstalls()) {
            startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
        }

        checkStart();

        checkUpdate();

        //



//        Gson gson = new GsonBuilder()
//                .serializeNulls()
//                .create();
//
//
//        LogS.printJson("MMMMMMM", gson.toJson(kk),"");
//      //  LogS.printJson("MMMMMMM", new Gson().toJson(kk),"");

    }

    private void checkUARTConnect() {
        Log.d("??????", "checkUARTConnect: isBootUp:" + isBootUp + ",isUartConnect:" + isUartConPortOpen);

        try {
            if (isUartConPortOpen) {
                //sub mcu??????
                checkTimer.timer(5000, number -> checkConnected());
                getDeviceInfo();
            } else {
                //port??????
                showErrorDialogAlert2();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RxTimer checkTimer = new RxTimer();
    private boolean isConnected = false;

    private void checkConnected() {
        if (!isConnected) {
            goNext();
        }
    }

    /**
     * ????????????
     */
    //1.??????????????????
    private void getDeviceInfo() {
        getInstance().commandDeviceInfo();
    }

    private void getAD() {

        Log.d("onInclineRead", "getAD: ");

        //?????? Incline AD
        if (MODE == ModeEnum.XE395ENT) {
            try {
                if (TextUtils.isEmpty(getInstance().getDeviceSettingBean().getDsInclineAd())) {
                    getInstance().commandReadIncline();
                } else if (getInstance().getDeviceSettingBean().getDsInclineAd().contains("0#0#0#0#0#0#0#")) {
                    getInstance().commandReadIncline();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //?????? Level AD
        if (TextUtils.isEmpty((getInstance().getDeviceSettingBean().getDsLevelAd()))) {
            int[] levelAdArray = MODE.getLevelAD();
            StringBuilder levelAdStr = new StringBuilder();
            for (int i = 0; i < levelAdArray.length; i++) {
                levelAdStr.append(i == 19 ? levelAdArray[i] : levelAdArray[i] + "#");
            }
            deviceSettingBean = getInstance().getDeviceSettingBean();
            deviceSettingBean.setDsLevelAd(levelAdStr.toString());
            getInstance().setDeviceSettingBean(deviceSettingBean);
        }

        goNext();
    }

    private void goNext() {
        isDone = true;

        try {
            if (deviceSettingBean.isFirst_launch()) {
                checkTimeZone();
                Navigation.findNavController(this, R.id.nhfStartScreen).navigate(R.id.firstLaunchSetDateFragment);
            } else {
                Navigation.findNavController(this, R.id.nhfStartScreen).navigate(R.id.startScreenFragment);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initEvent() {

        Disposable d = RxBus.getInstance().toObservable(MsgEvent.class).subscribe(msg -> {


            //onDestroy???????????????onResume???
            if (isNext) return;

            //1.??????????????????
            if (msg.getType() == ON_DEVICE_INFO) {
                isConnected = true;
                //??????????????????
                getInstance().commandSetLwrMode(Device.LWR_MODE.NORMAL);
            }

            //2.????????????
            if (msg.getType() == ON_LWR_MODE) {

                isConnected = true;
                if ((msg.getObj()) == Device.LWR_MODE.NORMAL) {
                    //??????AD
                    getAD();
                }
            }

            //error ??????????????????
            if (msg.getType() == ON_ERROR) {
                goNext();
                isConnected = true;
                if (checkRetry < 3) {
                    if (isDone) return;
                    getDeviceInfo();
                } else {
                    if (isDone) return;
                    showErrorDialogAlert((CommandErrorBean) msg.getObj());
                }

                checkRetry++;
            }
        });

        compositeDisposable.add(d);
    }


    int checkRetry = 0;
    boolean isDone = false;
    public boolean isNext = false;

    @Override
    protected void onResume() {
        super.onResume();

        checkUpdate();

//        if (!isSupported) {
//            showErrorDialogAlert3();
//         //   Toasty.warning(this,"THIS DEVICE IS NOT SUPPORTED!",Toasty.LENGTH_LONG).show();
//            return;
//        }
//
//        Log.d("??????", "onResume:isBootUp: " + isBootUp +", fragment_id:"+fragment_id);
//        if (fragment_id > 0) {
//            //Guest Create Profile
//            try {
//                Navigation.findNavController(this, R.id.nhfStartScreen).navigate(fragment_id);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        } else {
//            if (!isBootUp) {
//
//                new RxTimer().timer(2000, number -> checkUARTConnect());
//             //   checkUpdate();
//            } else {
//                if (deviceSettingBean.isFirst_launch()) {
//                    Navigation.findNavController(this, R.id.nhfStartScreen).navigate(R.id.firstLaunchSetDateFragment);
//                } else {
//                    Navigation.findNavController(this, R.id.nhfStartScreen).navigate(R.id.startScreenFragment);
//                }
//            }
//        }
    }

    private void checkStart() {
//        if (!isSupported) {
//            showErrorDialogAlert3();
//            return;
//        }

        Log.d("??????", "onResume:isBootUp: " + isBootUp + ", fragment_id:" + fragment_id);
        if (fragment_id > 0) {
            //Guest Create Profile
            try {
                Navigation.findNavController(this, R.id.nhfStartScreen).navigate(fragment_id);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            if (!isBootUp) {

                new RxTimer().timer(2000, number -> checkUARTConnect());
                //   checkUpdate();
            } else {
                if (deviceSettingBean.isFirst_launch()) {
                    Navigation.findNavController(this, R.id.nhfStartScreen).navigate(R.id.firstLaunchSetDateFragment);
                } else {
                    Navigation.findNavController(this, R.id.nhfStartScreen).navigate(R.id.startScreenFragment);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        isConnected = true;
        if (checkTimer != null) {
            checkTimer.cancel();
            checkTimer = null;
        }
        // removeFloatView();
        btnExitFullScreen.removeFloatView();
        btnExitFullScreen = null;

        if (checkUpdateRxTimer != null) {
            checkUpdateRxTimer.cancel();
            checkUpdateRxTimer = null;
        }


    }

    private Dialog errorDialog;

    public void showErrorDialogAlert(CommandErrorBean commandErrorBean) {
        runOnUiThread(() -> {
            if (errorDialog == null || !errorDialog.isShowing()) {
                errorDialog = new Dialog(MainActivity.this, android.R.style.ThemeOverlay);
                errorDialog.setCanceledOnTouchOutside(false);
                errorDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                errorDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                errorDialog.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                errorDialog.setContentView(R.layout.dialog_error_alert);
                TextView textView = errorDialog.findViewById(R.id.tv_ShowText);
                TextView textView2 = errorDialog.findViewById(R.id.tv_ShowText2);
                ImageView btn_close = errorDialog.findViewById(R.id.btn_close);
                Button btn_support = errorDialog.findViewById(R.id.btn_support);
                errorDialog.show();
                errorDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

                btn_close.setOnClickListener(v -> {
                    errorDialog.dismiss();
                    goNext();
                });
                btn_support.setOnClickListener(v -> {
                    errorDialog.dismiss();
                    goNext();
                });

                if (commandErrorBean.getErrorType() == 1) {
                    textView.setText("ERROR");
                    textView2.setText(commandErrorBean.getErrorMessage());
                } else {
                    textView.setText(commandErrorBean.getCommand().toString());
                    textView2.setText(commandErrorBean.getCommandError().toString());
                }
            }
        });
    }

    private Dialog errorDialog2;

    public void showErrorDialogAlert2() {
        if (errorDialog2 == null || !errorDialog2.isShowing()) {
            errorDialog2 = new Dialog(MainActivity.this, android.R.style.ThemeOverlay);
            errorDialog2.setCanceledOnTouchOutside(false);
            errorDialog2.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            errorDialog2.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            errorDialog2.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            errorDialog2.setContentView(R.layout.dialog_error_alert);
            TextView textView = errorDialog2.findViewById(R.id.tv_ShowText);
            TextView textView2 = errorDialog2.findViewById(R.id.tv_ShowText2);
            ImageView btn_close = errorDialog2.findViewById(R.id.btn_close);
            Button btn_support = errorDialog2.findViewById(R.id.btn_support);
            errorDialog2.show();
            errorDialog2.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            btn_close.setOnClickListener(v -> {
                goNext();
                errorDialog2.dismiss();
            });
            btn_support.setOnClickListener(v -> {
                goNext();
                errorDialog2.dismiss();
            });

            textView.setText("UART Not Connected.");
            textView2.setText("");
        }
    }

    public void showErrorDialogAlert3() {
        if (errorDialog2 == null || !errorDialog2.isShowing()) {
            errorDialog2 = new Dialog(MainActivity.this, android.R.style.ThemeOverlay);
            errorDialog2.setCanceledOnTouchOutside(false);
            errorDialog2.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            errorDialog2.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            errorDialog2.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            errorDialog2.setContentView(R.layout.dialog_error_alert);
            TextView textView = errorDialog2.findViewById(R.id.tv_ShowText);
            TextView textView2 = errorDialog2.findViewById(R.id.tv_ShowText2);

            errorDialog2.show();
            errorDialog2.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            textView.setText("THIS DEVICE IS NOT SUPPORTED!");
            textView2.setText("");
        }
    }

    /**
     * ??????app??????
     */
    private void checkUpdate() {
        Log.d("??????", "checkUpdate: ");
        BaseApi.request(BaseApi.createApi2(IServiceApi.class).apiCheckUpdate(),
                new BaseApi.IResponseListener<UpdateBean>() {
                    @Override
                    public void onSuccess(UpdateBean data) {
                        LogS.printJson("??????",new Gson().toJson(data),"");
                     //   Log.d("??????", "@@@data: " + data.toString());
                        try {

                            if (convertSwVersion(data.getOS_Version()) > checkSwVersion()) {
                                updateNotify = true;
                                return;
                            } else {
                                updateNotify = false;
                            }

                            if (data.getVersionCode() > new CommonUtils().getLocalVersionCode()) {
                                Log.d("??????", "????????????");
                                if (checkUpdateRxTimer != null) {
                                    checkUpdateRxTimer.cancel();
                                    checkUpdateRxTimer = null;
                                }

                                MyApplication.SSEB = false;
                                Intent intent = new Intent(MainActivity.this, UpdateSoftwareActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putString("fileUrl", data.getDownloadURL());
                                bundle.putString("md5", data.getMD5());
                                bundle.putBoolean("isForce", true);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFail() {
                        Log.d("WEB_API-checkUpdate", "??????");
                    }
                });
    }


    private void checkTimeZone() {
        BaseApi.request(BaseApi.createApi3(IServiceApi.class).apiGetTimeZone(),
                new BaseApi.IResponseListener<TimeZoneBean>() {
                    @Override
                    public void onSuccess(TimeZoneBean data) {
                        try {
                            Log.d("TIME_ZONE", "onSuccess: ");
                            setTimeZone(data.getGeoplugin_timezone());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d("TIME_ZONE", "try catch_onSuccess: ");
                            setTimeZone("Asia/Taipei");
                        }
                    }

                    @Override
                    public void onFail() {
                        Log.d("TIME_ZONE", "onFail: ");
                        setTimeZone("Asia/Taipei");
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void setTimeZone(String timeZone) {
        AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        am.setTimeZone(timeZone);
        Log.d("TIME_ZONE", "TIME_ZONE: " + TimeZone.getDefault().getDisplayName());
    }
}

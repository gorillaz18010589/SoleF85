package com.dyaco.spiritbike.workout;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dyaco.spiritbike.support.BaseFragment;
import com.dyaco.spiritbike.support.CommonUtils;
import com.dyaco.spiritbike.R;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.dyaco.spiritbike.MyApplication.DEFAULT_SEEK_VALUE_LEVEL;
import static com.dyaco.spiritbike.MyApplication.MODE;
import static com.dyaco.spiritbike.product_flavors.ModeEnum.XE395ENT;
import static com.dyaco.spiritbike.support.CommonUtils.incF2I;
import static com.dyaco.spiritbike.support.ProgramsEnum.HEART_RATE;
import static com.dyaco.spiritbike.support.ProgramsEnum.MANUAL;
import static com.dyaco.spiritbike.MyApplication.DEFAULT_SEEK_VALUE;


public class WorkoutDiagramFragment extends BaseFragment implements WorkoutDashboardActivity.OnInclinePlusListener, WorkoutDashboardActivity.OnLevelUpdateListener {

    private static final int INCLINE_RUNNING_01 = R.drawable.diagram_incline_running_01;
    private static final int INCLINE_RUNNING_02 = R.drawable.diagram_incline_running_02;
    private static final int INCLINE_FINISH_01 = R.drawable.diagram_incline_finish_01;
    private static final int INCLINE_FINISH_02 = R.drawable.diagram_incline_finish_02;
    private static final int INCLINE_PENDING_01 = R.drawable.diagram_incline_pending_01;
    private static final int INCLINE_PENDING_02 = R.drawable.diagram_incline_pending_02;

    private static final int LEVEL_RUNNING_01 = R.drawable.diagram_level_running_01;
    private static final int LEVEL_RUNNING_02 = R.drawable.diagram_level_running_02;
    private static final int LEVEL_FINISH_01 = R.drawable.diagram_level_finish_01;
    private static final int LEVEL_FINISH_02 = R.drawable.diagram_level_finish_02;
    private static final int LEVEL_PENDING_01 = R.drawable.diagram_level_pending_01;
    private static final int LEVEL_PENDING_02 = R.drawable.diagram_level_pending_02;

    private final int SEGMENT_PENDING = 0;
    private final int SEGMENT_RUNNING = 1;
    private final int SEGMENT_FINISH = 2;

    private final int DIAGRAM_TYPE_LEVEL = 0;
    private final int DIAGRAM_TYPE_INCLINE = 1;
    private final int DIAGRAM_TYPE_ALL = 2;

    public static int diagramSwitch;

    private View view;

    private int timeTick;
    private AlphaAnimation alphaAnimation;
   // int topLevel;
    WorkoutDashboardActivity parent;
    public int currentSegment = 0;

    public WorkoutDiagramFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (MODE == XE395ENT) {
            return inflater.inflate(R.layout.fragment_workout_diagram, container, false);
        } else {
            return inflater.inflate(R.layout.fragment_workout_diagram_bike, container, false);
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.view = view;

        parent = ((WorkoutDashboardActivity) mActivity);
        parent.setOnInclinePlusListener(this);
        parent.setOnLevelUpdateListener(this);

        initView();

        if (MODE == XE395ENT) {
            initSeekBar();
        } else {
            initLevelSeekBar();
        }

        //??????????????????
        setAllSeekBarUpdate(true);
    }

    private void initView() {

        //?????????
        diagramSwitch = DIAGRAM_TYPE_ALL;

        //Diagram????????????
        int timeOut = parent.workoutBean.getTimeSecond();
        if (timeOut != 0) {
            timeTick = timeOut / 20;
        } else {
            timeTick = 60;
            //    timeTick = 1;
        }

        //???????????????
        alphaAnimation = new AlphaAnimation(0.5f, 1.0f);
        alphaAnimation.setDuration(1000);
        alphaAnimation.setRepeatCount(Animation.INFINITE);
        alphaAnimation.setRepeatMode(Animation.RESTART);
    }

    /**
     * ?????????Level SeekBar
     * BIKE ?????????
     */
    synchronized private void initLevelSeekBar() {

        String valueLevel = parent.workoutBean.getLevelDiagramNum() == null || "".equals(parent.workoutBean.getLevelDiagramNum()) ? DEFAULT_SEEK_VALUE_LEVEL : parent.workoutBean.getLevelDiagramNum();
        int[] arrayLevel = Arrays.stream(valueLevel.split("#", -1))
                .mapToInt(Integer::parseInt)
                .toArray();
        for (int i = 0; i < 20; i++) {
            String seekBar02 = "sb_inc_pen_02_" + (i + 1);
            int resID02 = getResources().getIdentifier(seekBar02, "id", mActivity.getPackageName());
            //?????????
            int status = i == 0 ? SEGMENT_RUNNING : SEGMENT_PENDING;
            view.findViewById(resID02).setEnabled(false);
            parent.workoutBean.getDiagramLevelList().add(new DiagramBean(view.findViewById(resID02), null, status, arrayLevel[i], arrayLevel[i], arrayLevel[i], arrayLevel[i]));
            parent.workoutBean.getDiagramHRList().add(new DiagramBean(0));
        }

        switch (parent.PROGRAM_TYPE) {
            case HILL:
            case FATBURN:
            case CARDIO:
            case STRENGTH:
            case HIIT:
            case CUSTOM:
                //??????MaxLevel ???????????????LEVEL
                int maxLevel = parent.LEVEL_MAX; //??????Program??? ????????? MAX LEVEL
                Arrays.sort(arrayLevel);
                // level diagram??? ????????????????????????custom????????????5?????????????????????5
                int currentMaxLevel = MathUtils.clamp(arrayLevel[arrayLevel.length - 1], 5, 20);
             //   topLevel = currentMaxLevel;
                parent.setMaxLevel(parent.LEVEL_MAX);

                for (int i = 0; i < parent.workoutBean.getDiagramLevelList().size(); i++) {
                    DiagramBean diagramBean = parent.workoutBean.getDiagramLevelList().get(i);
                    int currentLevel = diagramBean.getProgressLevel();
                    int newLevel = CommonUtils.calcCurrentLevel(maxLevel, currentLevel, currentMaxLevel);
                    diagramBean.setProgressLevel(newLevel);

                    //????????????LEVEL??????SeekBar?????????????????????????????????????????????????????????
                 //   diagramBean.getSeekBar01().setMax(topLevel);

                    parent.workoutBean.getDiagramLevelList().set(i, diagramBean);
                }
        }
    }

    /**
     * ???????????????SeekBar
     * ???????????????
     */
    synchronized private void initSeekBar() {

        String valueLevel = parent.workoutBean.getLevelDiagramNum() == null || "".equals(parent.workoutBean.getLevelDiagramNum()) ? DEFAULT_SEEK_VALUE_LEVEL : parent.workoutBean.getLevelDiagramNum();
        String valueIncline = parent.workoutBean.getInclineDiagramNum() == null || "".equals(parent.workoutBean.getInclineDiagramNum()) ? DEFAULT_SEEK_VALUE : parent.workoutBean.getInclineDiagramNum();

        int[] arrayLevel = Arrays.stream(valueLevel.split("#", -1))
                .mapToInt(Integer::parseInt)
                .toArray();

        int[] arrayIncline = Arrays.stream(valueIncline.split("#", -1))
                .mapToInt(Integer::parseInt)
                .toArray();

        Log.d("VVVVV", "initSeekBar: " + valueIncline);

        //ALL
        for (int i = 0; i < 20; i++) {
            //   String seekBar02 = "sb_inc_pen_02_" + (i + 1);
            String seekBarLevel = "sb_all_level_" + (i + 1);
            String seekBarIncline = "sb_all_incline_" + (i + 1);
            //   int resID02 = getResources().getIdentifier(seekBar02, "id", mActivity.getPackageName());
            int resID1 = getResources().getIdentifier(seekBarLevel, "id", mActivity.getPackageName());
            int resID2 = getResources().getIdentifier(seekBarIncline, "id", mActivity.getPackageName());

            //?????????
            int status = i == 0 ? SEGMENT_RUNNING : SEGMENT_PENDING;

            view.findViewById(resID1).setEnabled(false);
            view.findViewById(resID2).setEnabled(false);

            parent.workoutBean.getDiagramLevelList().add(new DiagramBean(view.findViewById(resID1), null, status, arrayLevel[i], arrayLevel[i], arrayIncline[i], arrayIncline[i]));

            parent.workoutBean.getDiagramInclineList().add(new DiagramBean(view.findViewById(resID2), null, status, arrayLevel[i], arrayLevel[i], arrayIncline[i], arrayIncline[i]));

            parent.workoutBean.getDiagramHRList().add(new DiagramBean(0));
        }

        switch (parent.PROGRAM_TYPE) {
//            case MANUAL:
//                parent.setMaxIncline(parent.workoutBean.getOrgMaxIncline());
//                break;
            case HILL:
            case FATBURN:
            case CARDIO:
            case STRENGTH:
            case HIIT:
            case CUSTOM:
                //??????Program??? ????????? MAX LEVEL
                int maxLevel = parent.LEVEL_MAX;

                //????????????MaxLevel
                parent.setMaxLevel(maxLevel);

                // level diagram??? ????????????????????????custom????????????5?????????????????????5
                Arrays.sort(arrayLevel);
                int currentMaxLevel = MathUtils.clamp(arrayLevel[arrayLevel.length - 1], 5, 20);
              //  topLevel = currentMaxLevel;

                //??????MaxLevel ???????????????LEVEL
                for (int i = 0; i < parent.workoutBean.getDiagramLevelList().size(); i++) {

                    DiagramBean diagramBean = parent.workoutBean.getDiagramLevelList().get(i);

                    //??????????????????Level
                    int newLevel = CommonUtils.calcCurrentLevel(maxLevel, diagramBean.getProgressLevel(), currentMaxLevel);
                    diagramBean.setProgressLevel(newLevel);

                    //????????????LEVEL??????SeekBar?????????????????????????????????????????????????????????
                 //   diagramBean.getSeekBar01().setMax(topLevel);

                    parent.workoutBean.getDiagramLevelList().set(i, diagramBean);


                    /**Incline
                     * ##############
                     */
                    int maxIncline = parent.INCLINE_MAX;

                    // level diagram??? ????????????????????????custom????????????5?????????????????????5
                    Arrays.sort(arrayIncline);
                    int currentMaxIncline = MathUtils.clamp(arrayIncline[arrayIncline.length - 1], 5, 41);
                    DiagramBean diagramInclineBean = parent.workoutBean.getDiagramInclineList().get(i);
                    //??????????????????Level
                //    int newIncline = CommonUtils.calcCurrentLevel(maxIncline, diagramInclineBean.getProgressIncline(), currentMaxIncline);
                //    diagramInclineBean.setProgressIncline(newIncline);

                    diagramInclineBean.setProgressIncline(diagramInclineBean.getProgressInclineOrigin());

                    //????????????LEVEL??????SeekBar?????????????????????????????????????????????????????????
                    //   diagramInclineBean.getSeekBar01().setMax(topLevel);
                    parent.workoutBean.getDiagramInclineList().set(i, diagramInclineBean);

                    /**
                     * ##############
                     */


                    Log.d("JJJJJJJ", "initSeekBar: " + diagramInclineBean.getProgressIncline() + "," + diagramInclineBean.getProgressInclineOrigin());
                }
        }
    }

    /**
     * ??????????????????
     *
     * @param view SeekBar
     */
    synchronized private void setAlphaAnimation(View view) {

        if (view.getAnimation() == null) {
            view.startAnimation(alphaAnimation);
        }
    }

    /**
     * ??????SeekBar??? ProgressDrawable
     *
     * @param seekBar          SeekBar
     * @param progressDrawable Style
     */
    synchronized private void setProgressDrawable(SeekBar seekBar, int progressDrawable) {

        seekBar.setProgressDrawable(ContextCompat.getDrawable(mActivity, progressDrawable));
    }

    /**
     * ?????? Level Incline Diagram???????????????SeekBar
     */
    synchronized private void setAllSeekBarUpdate(boolean isFlow) {

        for (int i = 0; i < 20; i++) {

            DiagramBean diagramBeanIncline = null;

            DiagramBean diagramBeanLevel = parent.workoutBean.getDiagramLevelList().get(i);

            if (MODE == XE395ENT) {
                diagramBeanIncline = parent.workoutBean.getDiagramInclineList().get(i);
                diagramBeanIncline.getSeekBar01().setProgress(diagramBeanIncline.getProgressIncline());
            }

//            if (parent.PROGRAM_TYPE == MANUAL) {
//                //?????? MANUAL Level Diagram
//                diagramBeanLevel.getSeekBar01().setProgress(diagramBeanLevel.getProgressLevel());
//            } else {
//                //MANUAL ????????? level Diagram ????????????????????????
//                diagramBeanLevel.getSeekBar01().setProgress(diagramBeanLevel.getProgressLevelOrigin());
//            }

            diagramBeanLevel.getSeekBar01().setProgress(diagramBeanLevel.getProgressLevel());

            //??????Program Incline?????????????????????Incline Diagram????????????
          //  if (MODE == XE395ENT) {

              //  if (parent.PROGRAM_TYPE == MANUAL) {
                    //?????? MANUAL Incline Diagram
                //    diagramBeanIncline.getSeekBar01().setProgress(diagramBeanIncline.getProgressIncline());
              //  } else {
                    //MANUAL ????????? Incline Diagram ????????????????????????
               //     diagramBeanIncline.getSeekBar01().setProgress(diagramBeanIncline.getProgressInclineOrigin());
              //  }

                //    diagramBeanIncline.getSeekBar01().setProgress(diagramBeanIncline.getProgressIncline());
        //    }

            int statusStyleLevel = 0;
            int statusStyleIncline = 0;
            switch (diagramBeanLevel.getStatus()) {
                case SEGMENT_PENDING:
                    statusStyleLevel = MODE == XE395ENT ? LEVEL_PENDING_01 : LEVEL_PENDING_02;
                    statusStyleIncline = INCLINE_PENDING_01;

                    if (MODE == XE395ENT) {
                        assert diagramBeanIncline != null;
                        if (diagramBeanIncline.getSeekBar01().getAnimation() != null)
                            diagramBeanIncline.getSeekBar01().clearAnimation();
                    }

                    if (diagramBeanLevel.getSeekBar01().getAnimation() != null)
                        diagramBeanLevel.getSeekBar01().clearAnimation();

                    break;
                case SEGMENT_RUNNING:
                    statusStyleLevel = MODE == XE395ENT ? LEVEL_RUNNING_01 : LEVEL_RUNNING_02;
                    statusStyleIncline = INCLINE_RUNNING_01;

                    setAlphaAnimation(diagramBeanLevel.getSeekBar01());
                    if (MODE == XE395ENT) {
                        assert diagramBeanIncline != null;
                        setAlphaAnimation(diagramBeanIncline.getSeekBar01());
                    }

                    //???WorkoutDashboard?????????Level???Incline ???????????????Segment??????

                    if (parent.PROGRAM_TYPE != HEART_RATE) { //heart rate ???????????????
                        if (isFlow) {
                            parent.setCurrentLevel(diagramBeanLevel.getProgressLevel(), false);
                        }
                    }

                    if (MODE == XE395ENT) {
                        if (isFlow) {
                            assert diagramBeanIncline != null;
                            parent.setCurrentIncline(diagramBeanIncline.getProgressIncline(), false);
                        }
                    }

                    break;
                case SEGMENT_FINISH:
                    statusStyleLevel = MODE == XE395ENT ? LEVEL_FINISH_01 : LEVEL_FINISH_02;
                    statusStyleIncline = INCLINE_FINISH_01;

                    if (diagramBeanLevel.getSeekBar01().getAnimation() != null) {
                        diagramBeanLevel.getSeekBar01().clearAnimation();
                    }

                    if (MODE == XE395ENT) {
                        assert diagramBeanIncline != null;
                        if (diagramBeanIncline.getSeekBar01().getAnimation() != null)
                            diagramBeanIncline.getSeekBar01().clearAnimation();
                    }

                    break;
                default:
            }

            setProgressDrawable(diagramBeanLevel.getSeekBar01(), statusStyleLevel);

            if (MODE == XE395ENT) {
                assert diagramBeanIncline != null;
                setProgressDrawable(diagramBeanIncline.getSeekBar01(), statusStyleIncline);
            }
        }
    }

    /**
     * ???WorkoutDashboard ?????? Incline ?????????
     *
     * @param updateInclineNum ???????????????
     */
    @Override
    public void onInclineUpdate(int updateInclineNum, boolean beep) {

        if (parent.inclineError) return;

        //?????????Incline??????
        diagramSwitch = DIAGRAM_TYPE_LEVEL;

        int currentIncline = parent.getCurrentIncline();
        int newCurrentIncline = currentIncline + (updateInclineNum);

        Log.d("KKKKKKK", "currentIncline: " + currentIncline +","+ newCurrentIncline);
        float ccinc = newCurrentIncline == 0 ? 0.5f : newCurrentIncline;

        boolean isManual = false;
        switch (parent.PROGRAM_TYPE) {
            case MANUAL:
            case HEART_RATE:
                isManual = true;

                //ftms ??????????????????
                if (currentIncline == newCurrentIncline) {
                    parent.responseFtmsUpdateIncline(true);
                    return;
                }
                boolean checkValue = newCurrentIncline <= 40 && newCurrentIncline >= 0;
                if (checkValue) {
                    parent.setCurrentIncline(newCurrentIncline, beep);
                    updateIncline(updateInclineNum, isManual, 0, 0);
                } else {
                    parent.responseFtmsUpdateIncline(false);
                    return;
                }
              //  setAllSeekBarUpdate(false);
                break;
            case HILL:
            case FATBURN:
            case CARDIO:
            case STRENGTH:
            case HIIT:
            case CUSTOM:

                //ftms ??????????????????
                if (currentIncline == newCurrentIncline) {
                    parent.responseFtmsUpdateIncline(true);
                    return;
                }

                //??????????????? incline ??????40 ??????0 ??????
                if (newCurrentIncline > 40 || newCurrentIncline < 0) {
                    parent.responseFtmsUpdateIncline(false);
                    return;
                }

                //??? +???MaxIncline ????????????20
                if (parent.INCLINE_MAX >= 40 && updateInclineNum >= 0) {
                    parent.responseFtmsUpdateIncline(false);
                    return;
                }

             //   Log.d("KKKKKKK", "org: " + parent.workoutBean.getOrgMaxIncline() + ",current" + parent.INCLINE_MAX);
                //??? -???newMaxLevel ???????????? orgMaxLevel
//                if (parent.INCLINE_MAX <= parent.workoutBean.getOrgMaxIncline() && updateInclineNum < 0) {
//                    parent.responseFtmsUpdateIncline(false);
//                    return;
//                }

                for (int i = 0; i < parent.workoutBean.getDiagramInclineList().size(); i++) {
                    if (parent.workoutBean.getDiagramInclineList().get(i).getStatus() == SEGMENT_RUNNING)
                        currentSegment = i;
                }

                //????????????MaxIncline  , ??????Incline, ???????????? Incline , ??????????????? max Incline
             //   int newMaxIncline = CommonUtils.getNewMaxLevel(parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin(), ccinc, parent.workoutBean.getOrgMaxIncline());
              //  int newMaxIncline = CommonUtils.getNewMaxIncline(parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin(), ccinc, parent.workoutBean.getOrgMaxIncline());

                float orgIncline = parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin() == 0 ? 0.5f :parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin();
                int newMaxIncline = CommonUtils.getNewMaxIncline(orgIncline, ccinc, parent.workoutBean.getOrgMaxIncline());
             //   Log.d("KKKKKKK", "onInclineUpdate: " + parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin() +","+ ccinc +","+parent.workoutBean.getOrgMaxIncline());
             //   Log.d("KKKKKKK", "onInclineUpdate: " + orgIncline +","+ ccinc +","+parent.workoutBean.getOrgMaxIncline());
              //  Log.d("KKKKKKK", "newMaxIncline: " + newMaxIncline);

                // ???????????? newMaxLevel ?????? ??????20 ????????? orgMaxLevel
                if (newMaxIncline > 40) {
                    parent.responseFtmsUpdateIncline(false);
                    return;
                }

              //  Log.d("KKKKKKK", "getOrgMaxIncline(): " + parent.workoutBean.getOrgMaxIncline());

                for (int i = 0; i < parent.workoutBean.getDiagramInclineList().size(); i++) {
                    DiagramBean diagramBean = parent.workoutBean.getDiagramInclineList().get(i);
                    if (diagramBean.getStatus() == SEGMENT_RUNNING || diagramBean.getStatus() == SEGMENT_PENDING) {

                        //   int newPLevel = CommonUtils.calcCurrentLevel(newMaxIncline, diagramBean.getProgressInclineOrigin(), parent.workoutBean.getOrgMaxIncline());

                       // int newPLevel = CommonUtils.calcCurrentLevel(diagramBean.getProgressInclineOrigin(), ccinc, parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin());
                        int newPLevel = CommonUtils.calcCurrentLevel(diagramBean.getProgressInclineOrigin() == 0 ? 0.5f : diagramBean.getProgressInclineOrigin(), ccinc, orgIncline);

                        Log.d("KKKKKKK", "calcCurrentLevel: " +i +":"+ newPLevel);
                        //??????????????????newCurrentLevel?????????????????????1?????????20??????????????????
                        if (newPLevel < 0 || newPLevel > 40) {
                            parent.responseFtmsUpdateIncline(false);
                            return;
                        }
                    }
                }

                //??????WorkoutDashboard???MaxLevel???CurrentLevel
                parent.setCurrentIncline(newCurrentIncline, beep);
                //??????Diagram Level
                updateIncline(updateInclineNum, isManual, newMaxIncline, ccinc);

                parent.INCLINE_MAX = newMaxIncline;
                parent.setMaxIncline(parent.INCLINE_MAX);
        }

//        //ftms ??????????????????
//        Log.d("FTMSSS", "onInclineUpdate: " + currentIncline +","+ newCurrentIncline);
//        if (currentIncline == newCurrentIncline) {
//            parent.responseFtmsUpdateIncline(true);
//            return;
//        }
//
//        boolean checkValue = newCurrentIncline <= parent.INCLINE_MAX && newCurrentIncline >= 1;
//
//        if (checkValue) {
//            parent.setCurrentIncline(newCurrentIncline, true);
//            updateIncline(updateInclineNum, isManual);
//        } else {
//            parent.responseFtmsUpdateIncline(false);
//            return;
//        }
//


          setAllSeekBarUpdate(false);

    }

    /**
     * ???WorkoutDashboard ?????? Level ?????????
     *
     * @param updateLevelNum ??????
     */
    @Override
    public void onLevelUpdate(int updateLevelNum, boolean beep) {

        if (parent.levelError) return;
        //?????????Level??????
        diagramSwitch = DIAGRAM_TYPE_ALL;

        int currentLevel = parent.getCurrentLevel();
        int newLevel = currentLevel + (updateLevelNum);

        switch (parent.PROGRAM_TYPE) {

            case HEART_RATE:
            case MANUAL:

                Log.d("AAAAAA", "currentLevel: " + currentLevel + "," + newLevel);
                //ftms ??????????????????
                if (currentLevel == newLevel) {
                    parent.responseFtmsUpdateLevel(true);
                    return;
                }

                boolean checkValue = newLevel <= 20 && newLevel >= 1;
                if (checkValue) {
                    parent.setCurrentLevel(newLevel, beep);
                    updateLevel(updateLevelNum, true, 0, 0);
                } else {
                    parent.responseFtmsUpdateLevel(false);
                    return;
                }
              //  setAllSeekBarUpdate(false);
                break;
            case HILL:
            case FATBURN:
            case CARDIO:
            case STRENGTH:
            case HIIT:
            case CUSTOM:

                Log.d("AAAAAA", "currentLevel: " + currentLevel + "," + newLevel);
                //ftms ??????????????????
                if (currentLevel == newLevel) {
                    parent.responseFtmsUpdateLevel(true);
                    return;
                }

                //??????????????? Level ??????20 ??????1 ??????
                if (newLevel > 20 || newLevel < 1) {
                    parent.responseFtmsUpdateLevel(false);
                    return;
                }
                Log.d("AAAAAA", "update?????? level newlevel: " + newLevel);

                //current level ????????????MaxLevel ????????????20
                if (parent.LEVEL_MAX >= 20 && updateLevelNum > 0) {
                    parent.responseFtmsUpdateLevel(false);
                    return;
                }
//                //current level ????????????newMaxLevel ???????????? orgMaxLevel
//                if (parent.LEVEL_MAX <= parent.workoutBean.getOrgMaxLevel() && updateLevelNum < 0) {
//                    parent.responseFtmsUpdateLevel(false);
//                    return;
//                }

                for (int i = 0; i < parent.workoutBean.getDiagramLevelList().size(); i++) {
                    if (parent.workoutBean.getDiagramLevelList().get(i).getStatus() == SEGMENT_RUNNING)
                        currentSegment = i;
                }

                Log.d("AAAAAA", "??????current level:" + parent.workoutBean.getDiagramLevelList().get(currentSegment).getProgressLevelOrigin() + ",???current level" + currentLevel);
                //????????????MaxLevel  , ??????level, ???????????? level , ??????????????? max level
                //  int newMaxLevel = CommonUtils.getNewMaxLevel(currentLevel, newLevel, parent.LEVEL_MAX);
                int newMaxLevel = CommonUtils.getNewMaxLevel(parent.workoutBean.getDiagramLevelList().get(currentSegment).getProgressLevelOrigin(), newLevel, parent.workoutBean.getOrgMaxLevel());
                Log.d("AAAAAA", "??????currentSegment:" + currentSegment);

                // ???????????? newMaxLevel ?????? ??????20 ????????? orgMaxLevel
                if (newMaxLevel > 20) {
                    // if (newMaxLevel > 20 || newMaxLevel < parent.LEVEL_MAX) {
                    parent.responseFtmsUpdateLevel(false);
                    return;
                }
                //   Log.d("AAAAAA", "newMaxLevel: " + newMaxLevel + ", ???????????????Max level" + parent.workoutBean.getOrgMaxLevel() + ",?????????max level:" + parent.LEVEL_MAX);

                for (int i = 0; i < parent.workoutBean.getDiagramLevelList().size(); i++) {
                    DiagramBean diagramBean = parent.workoutBean.getDiagramLevelList().get(i);
                    if (diagramBean.getStatus() == SEGMENT_RUNNING || diagramBean.getStatus() == SEGMENT_PENDING) {

                        //  int newPLevel = CommonUtils.calcCurrentLevel(newMaxLevel, diagramBean.getProgressLevel(), parent.LEVEL_MAX);
                        //  int newPLevel = CommonUtils.calcCurrentLevel(newMaxLevel, diagramBean.getProgressLevelOrigin(), parent.workoutBean.getOrgMaxLevel());
                        int newPLevel = CommonUtils.calcCurrentLevel(diagramBean.getProgressLevelOrigin(), newLevel, parent.workoutBean.getDiagramLevelList().get(currentSegment).getProgressLevelOrigin());
                        //??????????????????newCurrentLevel?????????????????????1?????????20??????????????????
                        Log.d("AAAAAA", "@" + i + ":????????????Level: " + newPLevel);

//                        if (newPLevel < 1 || newPLevel > 20) {
//                            parent.responseFtmsUpdateLevel(false);
//                            //    Log.d("AAAAAA", "??????: " + newMaxLevel);
//                            return;
//                        }

                        if (newPLevel > 20) {
                            parent.responseFtmsUpdateLevel(false);
                            //    Log.d("AAAAAA", "??????: " + newMaxLevel);
                            return;
                        }
                    }
                }

                //??????WorkoutDashboard???MaxLevel???CurrentLevel
                parent.setCurrentLevel(newLevel, beep);
                //??????Diagram Level
                //  updateLevel(updateLevelNum, false, parent.LEVEL_MAX, newMaxLevel);
                updateLevel(updateLevelNum, false, parent.LEVEL_MAX, newLevel);

                parent.LEVEL_MAX = newMaxLevel;
                parent.setMaxLevel(parent.LEVEL_MAX);

                break;
        }

        setAllSeekBarUpdate(false);
    }

    /**
     * ??????Level???
     *
     * @param updateLevelNum  ?????????
     * @param isManual        ?????????MANUAL Program
     * @param currentMaxLevel ??????MaxLevel (manual?????????)
     * @param newLevel        ???MaxLevel (manual?????????)
     */
    synchronized private void updateLevel(int updateLevelNum, boolean isManual, int currentMaxLevel, int newLevel) {

        if (parent.workoutBean.getDiagramLevelList() == null) return;

        if (isManual) {
            //MANUAL Level ???????????????Segment???CurrentLevel ?????????
            for (int i = 0; i < parent.workoutBean.getDiagramLevelList().size(); i++) {
                DiagramBean diagramBean = parent.workoutBean.getDiagramLevelList().get(i);
                if (diagramBean.getStatus() == SEGMENT_RUNNING || diagramBean.getStatus() == SEGMENT_PENDING) {
                    diagramBean.setProgressLevel(diagramBean.getProgressLevel() + (updateLevelNum));
                    parent.workoutBean.getDiagramLevelList().set(i, diagramBean);
                }
            }
        } else { //five program

            //????????????????????????LEVEL
            for (int i = 0; i < parent.workoutBean.getDiagramLevelList().size(); i++) {
                DiagramBean diagramBean = parent.workoutBean.getDiagramLevelList().get(i);
                if (diagramBean.getStatus() == SEGMENT_RUNNING || diagramBean.getStatus() == SEGMENT_PENDING) {
                    //  int newLevel = CommonUtils.calcCurrentLevel(newMaxLevel, diagramBean.getProgressLevel(), currentMaxLevel);
                    // int newLevel = CommonUtils.calcCurrentLevel(newMaxLevel, diagramBean.getProgressLevelOrigin(), parent.workoutBean.getOrgMaxLevel());
                    int newPLevel = CommonUtils.calcCurrentLevel(diagramBean.getProgressLevelOrigin(), newLevel, parent.workoutBean.getDiagramLevelList().get(currentSegment).getProgressLevelOrigin());

                    diagramBean.setProgressLevel(Math.max(newPLevel, 1));
                    Log.d("WWWWWW", "updateLevel: " + diagramBean.getProgressLevelOrigin() + "," + newLevel + "," + parent.workoutBean.getDiagramLevelList().get(currentSegment).getProgressLevelOrigin() + "," + newPLevel);
                    //  Log.d("WWWWWW", "onLevelUpdate: "+i+"," + newPLevel);
                    parent.workoutBean.getDiagramLevelList().set(i, diagramBean);

                    //  Log.d("HHHHHH", "@" + i + "@" + "newMaxLevel: " + newMaxLevel + ", CurrentLevel:" + diagramBean.getProgressLevel() + ",NewCurrentLevel:" + newLevel + ",currentMaxLevel:" + currentMaxLevel);
                }
            }
        }


    }

    /**
     * ??????Incline??????
     *
     * @param updateInclineNum ???
     * @param isManual         ?????????MANUAL Program
     */
    synchronized private void updateIncline(int updateInclineNum, boolean isManual, int newMaxIncline, float ccinc) {

        if (isManual) {
            //MANUAL ????????????????????? ?????????
            for (int i = 0; i < 20; i++) {
                DiagramBean diagramBean = parent.workoutBean.getDiagramInclineList().get(i);
                if (diagramBean.getStatus() == SEGMENT_RUNNING || diagramBean.getStatus() == SEGMENT_PENDING) {
                    diagramBean.setProgressIncline(diagramBean.getProgressIncline() + (updateInclineNum));
                    parent.workoutBean.getDiagramInclineList().set(i, diagramBean);
                }
            }
        } else {
//            //?????????????????????
//            for (int i = 0; i < 20; i++) {
//                DiagramBean diagramBean = parent.workoutBean.getDiagramInclineList().get(i);
//                if (diagramBean.getStatus() == SEGMENT_RUNNING) {
//                    int progress = diagramBean.getProgressIncline() + (updateInclineNum);
//                    diagramBean.setProgressIncline(progress);
//                    parent.workoutBean.getDiagramInclineList().set(i, diagramBean);
//                    break;
//                }
//            }

            //????????????????????????LEVEL
            for (int i = 0; i < parent.workoutBean.getDiagramInclineList().size(); i++) {
                DiagramBean diagramBean = parent.workoutBean.getDiagramInclineList().get(i);
                if (diagramBean.getStatus() == SEGMENT_RUNNING || diagramBean.getStatus() == SEGMENT_PENDING) {
                    //  int newIncline = CommonUtils.calcCurrentLevel(newMaxIncline, diagramBean.getProgressInclineOrigin(), parent.workoutBean.getOrgMaxIncline());
                  //  int newPIncline = CommonUtils.calcCurrentLevel(diagramBean.getProgressInclineOrigin(), ccinc, parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin());
                    int newPIncline = CommonUtils.calcCurrentLevel(diagramBean.getProgressInclineOrigin() == 0 ? 0.5f : diagramBean.getProgressInclineOrigin(), ccinc, parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin()  == 0 ? 0.5f : parent.workoutBean.getDiagramInclineList().get(currentSegment).getProgressInclineOrigin());

                    diagramBean.setProgressIncline(newPIncline);
                    parent.workoutBean.getDiagramInclineList().set(i, diagramBean);

                    //  Log.d("HHHHHH", "@" + i + "@" + "newMaxLevel: " + newMaxLevel + ", CurrentLevel:" + diagramBean.getProgressLevel() + ",NewCurrentLevel:" + newLevel + ",currentMaxLevel:" + currentMaxLevel);
                }
            }
        }
    }

    /**
     * Segment???????????????
     */
    synchronized public void segmentFlow(long milliSeconds) {
        mActivity.runOnUiThread(() -> {
            long sec = TimeUnit.MILLISECONDS.toSeconds(milliSeconds);

            //  if (parent.workoutBean.getTimeSecond() == sec) return; //??????????????????????????????

            if (sec % timeTick == 0) {
                //  switchDiagram();
                for (int i = 0; i < 20; i++) {

                    //   if (parent.workoutBean.getDiagramInclineList().get(i).getStatus() == SEGMENT_RUNNING) {
                    if (parent.workoutBean.getDiagramLevelList().get(i).getStatus() == SEGMENT_RUNNING) {

                        //???hr
                        parent.workoutBean.getDiagramHRList().get(i).setHr(parent.mCurrentHR);

                        //??????????????????Running???Segment?????????Finish
                        if (MODE == XE395ENT)
                            parent.workoutBean.getDiagramInclineList().get(i).setStatus(SEGMENT_FINISH);

                        parent.workoutBean.getDiagramLevelList().get(i).setStatus(SEGMENT_FINISH);

                        //???????????????Segment??????????????????????????????
                        if (i != 19) {
                            if (MODE == XE395ENT)
                                parent.workoutBean.getDiagramInclineList().get(i + 1).setStatus(SEGMENT_RUNNING);

                            parent.workoutBean.getDiagramLevelList().get(i + 1).setStatus(SEGMENT_RUNNING);

                            //??????Segment
                            currentSegment = i + 1;

                        } else {
                            //????????????????????????Segment???????????????
                            if (parent.workoutBean.getTimeSecond() == 0) repeatDiagram();
                        }

                        setAllSeekBarUpdate(true);
                        //??????????????????
                        return;
                    }
                }
            }
        });
    }

    public int reCount = 1;
    /**
     * Diagram????????????
     */
    synchronized private void repeatDiagram() {

       // int size = parent.workoutBean.getDiagramInclineList().size();
        int size = 20;

        //MANUAL - ?????????????????????Segment??????
        if (parent.PROGRAM_TYPE == MANUAL) {
            int lastLevelProgress = parent.workoutBean.getDiagramLevelList().get(size - 1).getProgressLevel();
            int lastInclineProgress = 0;
            if (MODE == XE395ENT) {
                lastInclineProgress = parent.workoutBean.getDiagramInclineList().get(size - 1).getProgressIncline();
            }

            for (int i = 0; i < size; i++) {
                DiagramBean diagramBeanLevel = parent.workoutBean.getDiagramLevelList().get(i);

                if (MODE == XE395ENT) {
                    DiagramBean diagramBeanIncline = parent.workoutBean.getDiagramInclineList().get(i);
                    diagramBeanIncline.setStatus(i == 0 ? SEGMENT_RUNNING : SEGMENT_PENDING);
                    diagramBeanIncline.setProgressIncline(lastInclineProgress);
                    parent.workoutBean.getDiagramInclineList().set(i, diagramBeanIncline);
                }

                diagramBeanLevel.setStatus(i == 0 ? SEGMENT_RUNNING : SEGMENT_PENDING);
                diagramBeanLevel.setProgressLevel(lastLevelProgress);
                parent.workoutBean.getDiagramLevelList().set(i, diagramBeanLevel);
            }

        } else {

            for (int i = 0; i < size; i++) {
                DiagramBean diagramBeanLevel = parent.workoutBean.getDiagramLevelList().get(i);
                if (MODE == XE395ENT) {
                    DiagramBean diagramBeanIncline = parent.workoutBean.getDiagramInclineList().get(i);
                    diagramBeanIncline.setStatus(i == 0 ? SEGMENT_RUNNING : SEGMENT_PENDING);
                    parent.workoutBean.getDiagramInclineList().set(i, diagramBeanIncline);
                }
                diagramBeanLevel.setStatus(i == 0 ? SEGMENT_RUNNING : SEGMENT_PENDING);
                parent.workoutBean.getDiagramLevelList().set(i, diagramBeanLevel);
            }
        }

        reCount += 1;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //   if (countDownTimer != null) countDownTimer.cancel();

        view = null;
        alphaAnimation = null;
        parent = null;
    }


//    @Override
//    public void onHiddenChanged(boolean hidden) {
//        super.onHiddenChanged(hidden);
//        if (hidden) {
//        } else {
//        }
//    }
}
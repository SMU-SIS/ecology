package sg.edu.smu.ecology;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Created by anurooppv on 30/3/2017.
 */

/**
 * This class tracks the activity life cycles and hence ecology can query to know the current states
 * of the activities
 */
class ActivityLifecycleTracker implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = ActivityLifecycleTracker.class.getSimpleName();

    /**
     * The activity that is currently in the foreground
     */
    private Activity currentForegroundActivity;

    /**
     * Get the activity that is currently in the foreground
     *
     * @return the foreground activity
     */
    Activity getCurrentForegroundActivity() {
        return currentForegroundActivity;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentForegroundActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}

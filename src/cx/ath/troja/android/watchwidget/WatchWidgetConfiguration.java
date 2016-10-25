package cx.ath.troja.android.watchwidget;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.util.*;
import android.widget.*;
import android.view.*;
import android.text.*;

import java.io.*;

import com.darekxan.insanity.toolbox.*;

public class WatchWidgetConfiguration extends Activity {

    final static String PATH = "path";
    final static String REGEXP = "regexp";
    final static String REPLACEMENT = "replacement";
    final static String INTERVAL = "interval";
    final static String FONT_SIZE = "font_size";
    final static String FONT_COLOR = "font_color";
    final static String TYPE = "type";
    final static String BACKGROUND_COLOR = "background_color";
    final static String TYPE_FILE = "file";
    final static String TYPE_COMMAND = "command";
    final static String COMMAND = "command";

    final static int PICK_FILE_REQUEST = 1;

    private boolean fileSelected = false;
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	if (requestCode == PICK_FILE_REQUEST) {
	    if (data != null && data.getData() != null) {
		((Button) findViewById(R.id.watch_widget_configuration_watch_path_button)).setText(data.getData().getPath());
		((Button) findViewById(R.id.watch_widget_configuration_save_button)).setEnabled(true);
	    }
	}
    }

    private void hideCommandOptions() {
	findViewById(R.id.watch_widget_configuration_command_type).setVisibility(View.GONE);
	findViewById(R.id.watch_widget_configuration_file_type).setVisibility(View.VISIBLE);
	if (((Button) findViewById(R.id.watch_widget_configuration_watch_path_button)).getText().toString().equals(getApplicationContext().getResources().getString(R.string.watch_widget_watch_path))) {
	    ((Button) findViewById(R.id.watch_widget_configuration_save_button)).setEnabled(false);
	} else {
	    ((Button) findViewById(R.id.watch_widget_configuration_save_button)).setEnabled(true);
	}
    }

    private void hideFileOptions() {
	findViewById(R.id.watch_widget_configuration_command_type).setVisibility(View.VISIBLE);
	findViewById(R.id.watch_widget_configuration_file_type).setVisibility(View.GONE);
	if (((EditText) findViewById(R.id.watch_widget_configuration_watch_command)).getText().toString().equals("")) {
	    ((Button) findViewById(R.id.watch_widget_configuration_save_button)).setEnabled(false);
	} else {
	    ((Button) findViewById(R.id.watch_widget_configuration_save_button)).setEnabled(true);
	}
    }

    private int getWidgetId() {
        Intent launchIntent = getIntent();
        Bundle extras = launchIntent.getExtras();
        if (extras != null) {
            return extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
	}
	throw new RuntimeException("Illegally called");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	final int appWidgetId = getWidgetId();
	
	Intent cancelResultValue = new Intent();
	cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
	setResult(RESULT_CANCELED, cancelResultValue);
	
	setContentView(R.layout.watch_widget_configuration);

        final SharedPreferences config = getSharedPreferences(this.getClass().getName() + "." + appWidgetId, Context.MODE_PRIVATE);
	final Button saveButton = (Button) findViewById(R.id.watch_widget_configuration_save_button);

	final Button selectPathButton = (Button) findViewById(R.id.watch_widget_configuration_watch_path_button);
	selectPathButton.setText(config.getString(PATH, getApplicationContext().getResources().getString(R.string.watch_widget_watch_path)));
	final EditText watchCommandEdit = (EditText) findViewById(R.id.watch_widget_configuration_watch_command);
	watchCommandEdit.setText(config.getString(COMMAND, ""));
	watchCommandEdit.addTextChangedListener(new TextWatcher() {
		public void afterTextChanged(Editable e) {
		    if (e.toString().equals("")) {
			saveButton.setEnabled(false);
		    } else {
			saveButton.setEnabled(true);
		    }
		}
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	    });

	final Spinner watchTypeSpinner = (Spinner) findViewById(R.id.watch_widget_configuration_watch_type);
	ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this, R.array.watch_widget_watch_types_array, android.R.layout.simple_spinner_item);
	typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	watchTypeSpinner.setAdapter(typeAdapter);
	if (config.getString(TYPE, TYPE_FILE).equals(TYPE_FILE)) {
	    watchTypeSpinner.setSelection(0);
	} else {
	    watchTypeSpinner.setSelection(1);
	}
	watchTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		    if (position == 0) {
			WatchWidgetConfiguration.this.hideCommandOptions();
		    } else {
			WatchWidgetConfiguration.this.hideFileOptions();
		    }
		}
		public void onNothingSelected(AdapterView<?> parent) {
		}
	    });

	final EditText watchBackgroundColorEdit = (EditText) findViewById(R.id.watch_widget_configuration_watch_background_color);
	watchBackgroundColorEdit.setText("" + config.getString(BACKGROUND_COLOR, "88000000"));
	final EditText watchFontSizeEdit = (EditText) findViewById(R.id.watch_widget_configuration_watch_font_size);
	watchFontSizeEdit.setText("" + config.getInt(FONT_SIZE, 10));
	final EditText watchFontColorEdit = (EditText) findViewById(R.id.watch_widget_configuration_watch_font_color);
	watchFontColorEdit.setText("" + config.getString(FONT_COLOR, "ffffffff"));
        final EditText watchRegexpEdit = (EditText) findViewById(R.id.watch_widget_configuration_watch_regexp);
	watchRegexpEdit.setText(config.getString(REGEXP, ""));
        final EditText watchReplacementEdit = (EditText) findViewById(R.id.watch_widget_configuration_watch_replacement);
	watchReplacementEdit.setText(config.getString(REPLACEMENT, ""));
        final EditText watchIntervalEdit = (EditText) findViewById(R.id.watch_widget_configuration_watch_interval);
	watchIntervalEdit.setText("" + config.getInt(INTERVAL, 10));

	selectPathButton.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		    File startPath = null;
		    if (selectPathButton.getText().toString().equals(getApplicationContext().getResources().getString(R.string.watch_widget_watch_path))) {
			startPath = new File("/");
		    } else {
			startPath = new File(selectPathButton.getText().toString()).getParentFile();
		    }
		    intent.setDataAndType(Uri.fromFile(startPath), "vnd.android.cursor.dir/*");
		    WatchWidgetConfiguration.this.startActivityForResult(intent, PICK_FILE_REQUEST);
		}
	    });

        saveButton.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			setResult(RESULT_OK, resultValue);
			
			SharedPreferences.Editor configEditor = config.edit();
			if (watchTypeSpinner.getSelectedItem().equals("File")) {
			    configEditor.putString(TYPE, TYPE_FILE);
			} else {
			    configEditor.putString(TYPE, TYPE_COMMAND);
			}
			configEditor.putString(COMMAND, watchCommandEdit.getText().toString());
			configEditor.putString(PATH, selectPathButton.getText().toString());
			configEditor.putString(REGEXP, watchRegexpEdit.getText().toString());
			configEditor.putString(REPLACEMENT, watchReplacementEdit.getText().toString());
			configEditor.putInt(INTERVAL, Integer.parseInt(watchIntervalEdit.getText().toString()));
			configEditor.putInt(FONT_SIZE, Integer.parseInt(watchFontSizeEdit.getText().toString()));
			configEditor.putString(FONT_COLOR, watchFontColorEdit.getText().toString());
			configEditor.putString(BACKGROUND_COLOR, watchBackgroundColorEdit.getText().toString());
			configEditor.commit();
			sendBroadcast(WatchWidgetProvider.getWidgetUpdateIntent(WatchWidgetConfiguration.this, appWidgetId, false));
		    }
		    finish();
		}
	    });
    }

}
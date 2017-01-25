package com.delaroystudios.offlinesync;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.delaroystudios.offlinesync.R;

public class MainActivity extends ActionBarActivity {
	//DB Class to perform DB related operations
	DBController controller = new DBController(this);
	//Progress Dialog Object
	ProgressDialog prgDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//Get User records from SQLite DB 
		ArrayList<HashMap<String, String>> userList =  controller.getAllUsers();
		//
		if(userList.size()!=0){
			//Set the User Array list in ListView
			ListAdapter adapter = new SimpleAdapter( MainActivity.this,userList, R.layout.view_user_entry, new String[] { "userId","userName"}, new int[] {R.id.userId, R.id.userName});
			ListView myList=(ListView)findViewById(android.R.id.list);
			myList.setAdapter(adapter);
			//Display Sync status of SQLite DB
			Toast.makeText(getApplicationContext(), controller.getSyncStatus(), Toast.LENGTH_LONG).show();
		}
		//Initialize Progress Dialog properties
		prgDialog = new ProgressDialog(this);
		prgDialog.setMessage("Synching SQLite Data with Remote MySQL DB. Please wait...");
		prgDialog.setCancelable(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		//When Sync action button is clicked
		if (id == R.id.refresh) {
			//Sync SQLite DB data to remote MySQL DB
			syncSQLiteMySQLDB();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	//Add User method getting called on clicking (+) button
	public void addUser(View view) {
		Intent objIntent = new Intent(getApplicationContext(), NewUser.class);
		startActivity(objIntent);
	}
	
	public void syncSQLiteMySQLDB(){
		//Create AsycHttpClient object
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		ArrayList<HashMap<String, String>> userList =  controller.getAllUsers();
		if(userList.size()!=0){
			if(controller.dbSyncCount() != 0){
				prgDialog.show();
				params.put("usersJSON", controller.composeJSONfromSQLite());
				client.post("http://idsp.ak.gov.ng/offlinesync/insertuser.php",params ,new AsyncHttpResponseHandler() {
					@Override
					public void onSuccess(String response) {
						System.out.println(response);
						prgDialog.hide();
						try {
							JSONArray arr = new JSONArray(response);
							System.out.println(arr.length());
							for(int i=0; i<arr.length();i++){
								JSONObject obj = (JSONObject)arr.get(i);
								System.out.println(obj.get("id"));
								System.out.println(obj.get("status"));
								controller.updateSyncStatus(obj.get("id").toString(),obj.get("status").toString());
							}
							Toast.makeText(getApplicationContext(), "DB Sync completed!", Toast.LENGTH_LONG).show();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}
					}
		    
					@Override
					public void onFailure(int statusCode, Throwable error,
						String content) {
						// TODO Auto-generated method stub
						prgDialog.hide();
						if(statusCode == 404){
							Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
						}else if(statusCode == 500){
							Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
						}else{
							Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
						}
					}
				});
			}else{
				Toast.makeText(getApplicationContext(), "SQLite and Remote MySQL DBs are in Sync!", Toast.LENGTH_LONG).show();
			}
		}else{
				Toast.makeText(getApplicationContext(), "No data in SQLite DB, please do enter User name to perform Sync action", Toast.LENGTH_LONG).show();
		}
	}

}

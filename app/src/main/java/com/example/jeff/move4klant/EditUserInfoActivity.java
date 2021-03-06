package com.example.jeff.move4klant;

// manifest user licence
//<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import Objects.Category;
import library.DatabaseFunctions;
import library.DatabaseHandler;
import Objects.User;
import library.ServerRequestHandler;


public class EditUserInfoActivity extends Activity {

    private EditText etName, etLastName, etStreet, etPostalCode, etHouseNumber, etCity, etEmail;
    private Bitmap bitmap;
    private Bitmap croppedImage;
    private ImageView imageView;
    private DatabaseFunctions db;
    private User user;
    private int userID;
    private String oldFilePath;
    private String name, lastName, street, postalCode, houseNumber, city, email;
    private byte[] byteArray;
    private Bitmap[] output;
    private List<Category> savedLikes;
    private Boolean imageChanged = false;
    private Button changeCategory;
    private Boolean response = false;
    private String filePath;
    private final int REQUEST_PICK_CROP_IMAGE = 0;

    private ProgressDialog nDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_info);
        db = new DatabaseFunctions(getApplicationContext());
        getActionBar().setDisplayHomeAsUpEnabled(true);
        imageView = (ImageView)findViewById(R.id.ivProfileImageEdit);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        TableLayout table = (TableLayout)findViewById(R.id.tableViewCategory_EditUserInfo);
        user = DatabaseHandler.getInstance(getApplicationContext()).getUser();
        userID = user.getUserID();


        etName          = (EditText)findViewById(R.id.etName);
        etLastName      = (EditText)findViewById(R.id.etLastName);
//        etStreet        = (EditText)findViewById(R.id.etStreet);
//        etHouseNumber   = (EditText)findViewById(R.id.etHouseNumber);
//        etPostalCode    = (EditText)findViewById(R.id.etPostalCode);
//        etCity          = (EditText)findViewById(R.id.etCity);
        etEmail         = (EditText)findViewById(R.id.etEmail);

        // set db info in Edit text
        etName.setHint(user.getName());
        etLastName.setHint(user.getLastName());
//        etStreet.setHint(user.getStreet());
//        etHouseNumber.setHint(user.getHouseNumber());
//        etPostalCode.setHint(user.getPostalCode());
//        etCity.setHint(user.getCity());
        etEmail.setHint(user.getEmail());

        if (user.getFilePath().equals("")){
            oldFilePath = "";
            Bitmap defaultImage = BitmapFactory.decodeResource(getResources(), R.drawable.emptyprofile);
            Bitmap roundedDefaultImage = this.roundCornerImage(defaultImage, 15);
            imageView.setImageBitmap(roundedDefaultImage);
        }
        else {
            File imgFile = new  File(user.getFilePath());

            if(imgFile.exists() && imgFile != null){
                    bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if(bitmap != null)
                {
                    user.setImage(bitmap);
                    croppedImage = bitmap;
                    oldFilePath = user.getFilePath();
                    Bitmap userImage = this.roundCornerImage(user.getImage(), 15);
                    imageView.setImageBitmap(userImage);
                }
                else
                {
                    Bitmap defaultImage = BitmapFactory.decodeResource(getResources(), R.drawable.emptyprofile);
                    Bitmap roundedDefaultImage = this.roundCornerImage(defaultImage, 15);
                    imageView.setImageBitmap(roundedDefaultImage);
                }
            }
        }

        changeCategory = (Button)findViewById(R.id.btChangeCategory);

        savedLikes = DatabaseHandler.getInstance(getApplicationContext()).getLikedCategories();

        TableLayout tl = new TableLayout(this);

        for (Category category : savedLikes) {
            TableRow tr = new TableRow(this);
            Log.v("Category", category.getName());
            TextView label = new TextView(this);
            label.setTextSize(16);
            label.setText(category.getName());
            tr.addView(label);

            tl.addView(tr);
        }
        table.addView(tl);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_user_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.saveUserInfo:
                if ( isOnline()) {

                    saveUserDetails();
                    Intent i = new Intent(getApplicationContext(), ManageAccount.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    imageChanged = false;
                    DatabaseHandler.getInstance(getApplicationContext()).loadImages();
                    startActivity(i);
                    onBackPressed();
                }

                else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(EditUserInfoActivity.this);
                    alert.setTitle("Geen connectie");
                    alert.setMessage("Controleer uw verbinding");
                    // Set an EditText view to get user input
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                    }});
                    alert.show();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivProfileImageEdit:

                String status = Environment.getExternalStorageState();
                if (status.equals(Environment.MEDIA_MOUNTED)) {
                    File tempFile = new File(                                            Environment.getExternalStorageDirectory() + "/temp.jpg");
                    Uri tempUri = Uri.fromFile(tempFile);
                    Intent pickCropImageIntent = new Intent(
                            Intent.ACTION_PICK,                                           android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    pickCropImageIntent.setType("image/jpeg");
                    pickCropImageIntent.putExtra("crop", "true")
                            .putExtra("aspectX", 1)
                            .putExtra("aspectY", 1)
                            .putExtra("outputX", 200)
                            .putExtra("outputY", 200)
                            .putExtra("scale", "true");
                    pickCropImageIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            tempUri);
                    pickCropImageIntent.putExtra("outputFormat",
                            Bitmap.CompressFormat.JPEG.toString());
                    startActivityForResult(pickCropImageIntent,
                            REQUEST_PICK_CROP_IMAGE);
                }
                imageChanged = true;
                break;
            case R.id.btChangeCategory:
                saveUserDetails();
                Intent intent2 = new Intent(getApplicationContext(), LikesActivity.class);
                startActivity(intent2);
                finish();
                break;
        }
        // Inflate the menu; this adds items to the action bar if it is present.
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {

            case REQUEST_PICK_CROP_IMAGE:
                croppedImage = BitmapFactory.decodeFile(Environment
                        .getExternalStorageDirectory() + "/temp.jpg");
                if ( croppedImage != null) {
                    imageView.setImageBitmap(croppedImage);
                   // saveImageToSD(croppedImage);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byteArray = stream.toByteArray();
                }
                break;
        }
    }

    //@Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        nDialog = new ProgressDialog(EditUserInfoActivity.this);
//        nDialog.setTitle("Verwerken");
//        nDialog.setMessage("Loading..");
//        nDialog.setIndeterminate(false);
//        nDialog.setCancelable(true);
//        nDialog.show();
//
//        // TODO Auto-generated method stub
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (resultCode == RESULT_OK){
//
//
//            Uri targetUri = data.getData();
//            // save image to sd card
//            boolean success = false;
//            File sdCardDirectory = Environment.getExternalStorageDirectory();
//            File image = new File(sdCardDirectory, "ProfileImage.jpeg");
//            FileOutputStream outStream;
//            try {
//                // get image from selection
//                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
//                imageView.setImageBitmap(bitmap);
//                //saveImageToSD(bitmap);
//
//                // save to sd
//                saveImageToSD(bitmap);
//
//                // if correctly saved, show message
//                if (response) {
//                    // Show a toast message on successful save
//                    Toast.makeText(getApplicationContext(), "Image Saved to SD Card",
//                            Toast.LENGTH_SHORT).show();
//                }
//
//                // set image to this view
//                user.setImage(bitmap);
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                byteArray = stream.toByteArray();
//
//                //user.setImage(bitmap);
//
//
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
////            catch (IOException e) {
////                e.printStackTrace();
////            }
////
////            if (success) {
////                Toast.makeText(getApplicationContext(), "Image saved with success",
////                        Toast.LENGTH_LONG).show();
////            } else {
////                Toast.makeText(getApplicationContext(),
////                        "Error during image saving", Toast.LENGTH_LONG).show();
////            }
//
//
//
//        }
//    }

    @Override
    public void onBackPressed(){
        finish();
        this.overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);
    }



    public void saveUserDetails() {

        name            = etName.getText().toString();
        lastName        = etLastName.getText().toString();
//                street          = etStreet.getText().toString();
//                houseNumber     = etHouseNumber.getText().toString();
//                postalCode      = etPostalCode.getText().toString();
//                city            = etCity.getText().toString();
        email           = etEmail.getText().toString();
        // if input is empty, get last db info
        if (name.matches("")){
            name = etName.getHint().toString();
        }
        if (lastName.matches("")){
            lastName = etLastName.getHint().toString();
        }
//                if (street.matches("")){
//                    street = etStreet.getHint().toString();
//                }
//                if (houseNumber.matches("")){
//                    houseNumber = etHouseNumber.getHint().toString();
//                }
//                if (postalCode.matches("")){
//                    postalCode = etPostalCode.getHint().toString();
//                }
//                if (city.matches("")){
//                    city = etCity.getHint().toString();
//                }
        if (email.matches("")){
            email = etEmail.getHint().toString();
        }
        // reset values of the user
        user = new User(getApplicationContext(),userID, name, lastName, "", "", "", "", email);
        if (imageChanged){
            user.setFilePath(filePath);
            user.setImage(croppedImage);
        }else {
            user.setFilePath(oldFilePath);
        }
        DatabaseHandler.getInstance(getApplicationContext()).addUser(user.getUserID(), user.getName(), user.getLastName(), user.getEmail(), user.getFilePath());
        DatabaseHandler.getInstance(getApplicationContext()).uploadUserEditedInfo(user.getUserID(), user.getName(), user.getLastName(), user.getEmail());
        if (byteArray != null) {
            DatabaseHandler.getInstance(getApplicationContext()).uploadUserImage(user.getUserID(), byteArray);
        }


    }

    public Bitmap roundCornerImage(Bitmap src, float round) {
        // Source image size
        int width = src.getWidth();
        int height = src.getHeight();
        // create result bitmap output
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // set canvas for painting
        Canvas canvas = new Canvas(result);
        canvas.drawARGB(0, 0, 0, 0);

        // configure paint
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);

        // configure rectangle for embedding
        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);

        // draw Round rectangle to canvas
        canvas.drawRoundRect(rectF, round, round, paint);

        // create Xfer mode
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // draw source image to canvas
        canvas.drawBitmap(src, rect, rect, paint);

        // return final image
        return result;
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }



}


package android.example.purpose;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

public class PhotoConverterActivity extends AppCompatActivity {

    private LinearLayout selectImageLayout, convertingAndResultLayout, saveToGalleryLayout;

    private Button startConvertBtn;

    private RadioButton jpgBtn, pngBtn, webpBtn;

    private static final int PICKFILE_RESULT_CODE = 1;

    private Uri imageUri;

    private ImageView imageView;

    private TextView infoTextView;

    FirebaseStorage storage;
    StorageReference storageReference;

    Bitmap bmp;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_converter);

        //layouts declaration
        selectImageLayout = findViewById(R.id.select_photo_view);
        convertingAndResultLayout = findViewById(R.id.converting_and_result_view);
        saveToGalleryLayout = findViewById(R.id.save_to_gallery_view);

        //Buttons declaration
        startConvertBtn = findViewById(R.id.start_convert_button);
        Button homeBtn = findViewById(R.id.home_btn);

        //RadioButtons declaration
        jpgBtn = findViewById(R.id.to_jpg);
        pngBtn = findViewById(R.id.to_png);
        webpBtn = findViewById(R.id.to_webp);

        imageView = findViewById(R.id.image_view);
        infoTextView = findViewById(R.id.info_text_view);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();


        ActionBar actionBar;
        actionBar = getSupportActionBar();
        ColorDrawable colorDrawable
                = new ColorDrawable(
                Color.parseColor("#0F9D58"));
        actionBar.setBackgroundDrawable(colorDrawable);

        selectImageLayout.setOnClickListener(view -> selectImageFile());

        homeBtn.setOnClickListener(view -> {
            convertingAndResultLayout.setVisibility(View.GONE);
            saveToGalleryLayout.setVisibility(View.GONE);
            selectImageLayout.setVisibility(View.VISIBLE);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICKFILE_RESULT_CODE)
        {
            selectImageLayout.setVisibility(View.GONE);
            infoTextView.setVisibility(View.GONE);

            convertingAndResultLayout.setVisibility(View.VISIBLE);

            imageUri = data.getData();
            imageView.setImageURI(imageUri);

            startConvertBtn.setOnClickListener(view -> {
                if(jpgBtn.isChecked())
                {
                    convertToJpg(imageUri);
                    uploadImageFile();
                    convertingAndResultLayout.setVisibility(View.GONE);
                    saveToGalleryLayout.setVisibility(View.VISIBLE);
                }
                else if(pngBtn.isChecked())
                {
                    convertToPng(imageUri);
                    uploadImageFile();
                    convertingAndResultLayout.setVisibility(View.GONE);
                    saveToGalleryLayout.setVisibility(View.VISIBLE);
                }
                else if(webpBtn.isChecked())
                {
                    convertToWebp(imageUri);
                    uploadImageFile();
                    convertingAndResultLayout.setVisibility(View.GONE);
                    saveToGalleryLayout.setVisibility(View.VISIBLE);
                }
                else
                {
                    Toast.makeText(PhotoConverterActivity.this, "Please select Format!!!", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private void selectImageFile()
    {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
    }


    private void uploadImageFile()
    {
        if(imageUri !=null)
        {
            // Code for showing progressDialog while uploading
            ProgressDialog progressDialog
                    = new ProgressDialog(this);
            progressDialog.setTitle("Converting...");
            progressDialog.show();

            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
            ref.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                progressDialog.dismiss();
                Toast.makeText(PhotoConverterActivity.this, "Success!!! Image converted", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(PhotoConverterActivity.this, "Failed while converting image...", Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(snapshot -> {
                double progress
                        = (100.0
                        * snapshot.getBytesTransferred()
                        / snapshot.getTotalByteCount());
                progressDialog.setMessage(
                        "Converted "
                                + (int)progress + "%");
            });
        }
    }

    private String getNameFromUri(Uri imageUri)
    {
        Cursor c = getContentResolver().query(imageUri, null, null, null, null);
        c.moveToFirst();
        return c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
    }


    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void convertToJpg(Uri imageUri)
    {
        try
        {
            Toast.makeText(PhotoConverterActivity.this, "Converting started to JPEG format...", Toast.LENGTH_SHORT).show();

            String name = getNameFromUri(imageUri);

            bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

            //Save into gallery
            String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/Smart_Converter";
            File myDir = new File(root);
            myDir.mkdir();
            String fileName = "sic_" + name + ".jpg";
            File file = new File(myDir, fileName);
            System.out.println(file.getAbsolutePath());
            if (file.exists()) file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 50, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(PhotoConverterActivity.this, "Error: " + e, Toast.LENGTH_LONG).show();
            }

            MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, new String[]{"image/jpeg"}, null);
        }
        catch (Exception e)
        {
            Toast.makeText(PhotoConverterActivity.this, "Error:  "+ e , Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void convertToPng(Uri imageUri)
    {
        try
        {
            Toast.makeText(PhotoConverterActivity.this, "Converting started to Png Format", Toast.LENGTH_SHORT).show();

            String name = getNameFromUri(imageUri);

            bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

            //Save into gallery
            String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/Smart_Converter";
            File myDir = new File(root);
            myDir.mkdir();
            String fileName = "sic_" + name + ".png";
            File file = new File(myDir, fileName);
            System.out.println(file.getAbsolutePath());
            if (file.exists()) file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.PNG, 50, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(PhotoConverterActivity.this, "Error: " + e, Toast.LENGTH_LONG).show();
            }

            MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, new String[]{"image/png"}, null);
        }
        catch (Exception e)
        {
            Toast.makeText(PhotoConverterActivity.this, "Error: "+ e , Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void convertToWebp(Uri imageUri)
    {
        try
        {
            Toast.makeText(PhotoConverterActivity.this, "Converting started to Webp Format", Toast.LENGTH_SHORT).show();

            String name = getNameFromUri(imageUri);

            bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

            //Save into gallery
            String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/Smart_Converter";
            File myDir = new File(root);
            myDir.mkdir();
            String fileName = "sic_" + name + ".webp";
            File file = new File(myDir, fileName);
            System.out.println(file.getAbsolutePath());
            if (file.exists()) file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.WEBP, 40, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(PhotoConverterActivity.this, "Error: " + e, Toast.LENGTH_LONG).show();
            }

            MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, new String[]{"image/webp"}, null);
        }
        catch (Exception e)
        {
            Toast.makeText(PhotoConverterActivity.this, "Error: "+ e , Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


}
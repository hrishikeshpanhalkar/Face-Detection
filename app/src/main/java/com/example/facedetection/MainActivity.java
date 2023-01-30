package com.example.facedetection;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView1, imageView2;
    private Button button, button1;
    private static final String TAG = "MainActivity";
    private static final int SCALING_FACTOR = 10;
    private FaceDetector detector;
    private String currentPhotoPath;
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView1 = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.resultImageView);
        button = findViewById(R.id.detectFace);
        button1 = findViewById(R.id.captureImage);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission(Manifest.permission.CAMERA, CAMERA_REQUEST);
            }
        });
        FaceDetectorOptions realTimeFace =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .build();

        detector = FaceDetection.getClient(realTimeFace);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);

//                Uri imageUri = null;
//                try{
//                    Bitmap bitmap1 = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//                }catch (IOException e){
//                    e.printStackTrace();
//                }
                BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView1.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                analyzePhoto(bitmap);
            }
        });
    }

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
        } else {
            String fileName = "photo";
            File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);
                currentPhotoPath = imageFile.getAbsolutePath();

                Uri uri = FileProvider.getUriForFile(MainActivity.this, "com.example.facedetection.fileprovider", imageFile);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, CAMERA_REQUEST);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_PERMISSION_CODE) {

            // Checking whether user granted the permission or not.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                String fileName = "photo";
                File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                try {
                    File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);
                    currentPhotoPath = imageFile.getAbsolutePath();

                    Uri uri = FileProvider.getUriForFile(MainActivity.this, "com.example.facedetection.fileprovider", imageFile);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    startActivityForResult(intent, CAMERA_REQUEST);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = BitmapFactory.decodeFile(currentPhotoPath);
            imageView1.setImageBitmap(photo);
        }
    }

    private void analyzePhoto(Bitmap bitmap) {
        Log.d(TAG, "analyzePhoto: ");


        Bitmap smallerBitmap = Bitmap.createScaledBitmap(
                bitmap,
                bitmap.getWidth() / SCALING_FACTOR,
                bitmap.getHeight() / SCALING_FACTOR,
                false
        );

        InputImage inputImage = InputImage.fromBitmap(smallerBitmap, 0);
        Log.d(TAG, "inputImage: No of faces detected: " + inputImage.getHeight() + " " + inputImage.getWidth());
        detector.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        Log.d(TAG, "onSuccess: No of faces detected: " + faces.size());
                        for (Face face : faces) {
                            Rect rect = face.getBoundingBox();
                            rect.set(rect.left * SCALING_FACTOR,
                                    rect.top * (SCALING_FACTOR - 1),
                                    rect.right * SCALING_FACTOR,
                                    (rect.bottom * SCALING_FACTOR) + 90
                            );
                        }
                        cropDetectedFaces(bitmap, faces);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        Toast.makeText(MainActivity.this, "Detection failed due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cropDetectedFaces(Bitmap bitmap, List<Face> faces) {
        Log.d(TAG, "cropDetectedFaces: " + faces.size());
        if(!faces.isEmpty()){
            Rect rect = faces.get(0).getBoundingBox();

            int x = Math.max(rect.left, 0);
            int y = Math.max(rect.top, 0);
            int width = rect.width();
            int height = rect.height();

            Bitmap croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    x,
                    y,
                    (x + width > bitmap.getWidth()) ? bitmap.getWidth() - x : width,
                    (y + height > bitmap.getHeight()) ? bitmap.getHeight() - y : height
            );

            imageView2.setImageBitmap(croppedBitmap);
        }else {
            Toast.makeText(MainActivity.this, "No face Detected!!", Toast.LENGTH_SHORT).show();
        }
    }
}
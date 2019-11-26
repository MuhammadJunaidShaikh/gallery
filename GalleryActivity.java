package com.example.checkevemaster.gallery;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.example.checkevemaster.R;
import com.example.checkevemaster.preferencesclasses.UserAdverts;
import com.example.checkevemaster.preferencesclasses.UserSession;
import com.example.checkevemaster.resourceclasses.FinalClass;
import com.example.checkevemaster.resourceclasses.MySingleton;
import com.example.checkevemaster.resourceclasses.OtherConstants;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GalleryActivity extends AppCompatActivity implements GalleryRecycler.clicks {
    private GalleryRecycler imageAdapter;
    private UserSession userSession;
    private FinalClass fc;
    private Context context;
    private FirebaseFirestore db;
    //image selection
    private ArrayList<String> selectedImages;
    private static final int IMAGE_PICK_REQUEST = 200;
    private List<GalleryModel> imageList;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private Handler handler;
    private UserAdverts userAdverts;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        progressBar = findViewById(R.id.advertProfileProgressBar);
        progressBar.setVisibility(View.VISIBLE);
        handler = new Handler();
        imageList = new ArrayList<>();
        context = this;
        userSession = MySingleton.getInstance().getUser(context);
        fc = MySingleton.getInstance().getFinalClass();
        userAdverts = MySingleton.getInstance().getUserAdverts(context);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        imageList = new ArrayList<>();
        selectedImages = new ArrayList<>();
        imageAdapter = new GalleryRecycler(context, imageList, this);

        progressDialog = new ProgressDialog(context);
        initOtherViews();
    }

    private synchronized void initOtherViews() {
        //recycler
        RecyclerView recyclerView = findViewById(R.id.galleryActivityImageRecycler);
        recyclerView.setHasFixedSize(true);
        GridLayoutManager manager = new GridLayoutManager(context, 3);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(imageAdapter);

        //gallery button
        FloatingActionButton addImageFAB = findViewById(R.id.advertProfileAddPictureFAB);
        addImageFAB.setOnClickListener(onAddImageButtonClick);
        // upload buttton
        ImageButton uploadButton = findViewById(R.id.advertProfileUploadButton);
        uploadButton.setVisibility(View.GONE);
        uploadButton.setOnClickListener(onUploadButtonClick);


    }

    private View.OnClickListener onUploadButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            progressDialog.setMessage("Uploading slideshow... " + progrees + "%");

            progressDialog.setCancelable(false);
            progressDialog.show();
            uploadImages();
        }
    };

    private View.OnClickListener onAddImageButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICK_REQUEST);

        }
    };

    private int counter = 0;

    private int progrees = 0;

    private synchronized void uploadImages() {

        String advert = userSession.getUserId();
        if (advert != null && selectedImages != null) {
            for (String path : selectedImages) {
                GalleryModel model = new GalleryModel();
                model.setUploadTime(System.currentTimeMillis());
                model.setImageURL(path);
                model.setImageID(System.currentTimeMillis() + "_" + counter + ".jpeg");
                counter++;
                uploadImage(advert, model);
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    progrees = 0;
                    progressDialog.cancel();
                    fc.setToast("Images uploaded!", context);
                }
            }, 10000);


        } else
            fc.setToast("No images selected!", context);
    }


    private synchronized void uploadImage(final String advert, final GalleryModel model) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        Uri img = Uri.parse(model.getImageURL());
        final StorageReference file = storage.getReference(OtherConstants.ADVERT_STORAGE)
                .child(advert).child(OtherConstants.GALLERY)
                .child(model.getImageID());

        file.putFile(img)

                .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (!task.isSuccessful()) {
                            if (task.getException() != null) {

                                fc.setToast("Image format not supported", context);
                                task.getException().printStackTrace();
                                return null;
                            }
                            return null;
                        } else
                            return file.getDownloadUrl();

                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            model.setImageURL(String.valueOf(task.getResult()));
                            progrees += 30;
                            progressDialog.setMessage("Uploading slideshow... " + progrees + "%");
                            pushUrl(advert, model);

                        }


                    }
                });
    }

    private void pushUrl(final String advert, final GalleryModel model) {

        db.collection(OtherConstants.ADVERTISEMENT).document(advert).collection(OtherConstants.GALLERY)
                .document()
                .set(model)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull final Task<Void> task) {
                        if (task.isSuccessful()) {
                            progrees += 3;
                            progressDialog.setMessage("Uploading slideshow... " + progrees + "%");
                            progressDialog.setProgress(progrees);
                        }
                    }

                });


    }


    private synchronized void getImages() {
        String uid = userSession.getUserId();
        if (uid != null) {
            db.collection(OtherConstants.ADVERTISEMENT).document(uid).collection(OtherConstants.GALLERY)
                    .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    imageList = queryDocumentSnapshots.toObjects(GalleryModel.class);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            imageAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            fc.setToast(e.getLocalizedMessage(), context);
                        }
                    });
                }
            });

        } else
            handler.post(new Runnable() {
                @Override
                public void run() {
                    fc.setToast("Check internet connection!", context);
                }
            });
    }

    private void deleteImage(final GalleryModel model) {
        imageAdapter.notifyDataSetChanged();
        fc.setToast("Image Deleted!", context);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                db.collection(fc.getAdvertKey()).document(userSession.getUserId())
                        .collection(fc.getAdvertOtherData())
                        .document(fc.getAdvertMedia())
                        .update("models", FieldValue.arrayRemove(model));

                FirebaseStorage store = FirebaseStorage.getInstance();

                store.getReference(OtherConstants.ADVERT_STORAGE).child(userSession.getUserId()
                ).child("Media")
                        .child(model.getImageID()).delete();

            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getClipData() != null) {
                int count = data.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                //total selected images should be less than 4
                if (count < 4 && count > 0) {
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        addNewImageIntoList(String.valueOf(imageUri));
                    }
                } else {
                    fc.setToast("Max limit of images is 3!", context);
                }


            }

        }
    }

    private void addNewImageIntoList(final String uri) {

        GalleryModel updateModel = new GalleryModel();
        updateModel.setImageID(System.currentTimeMillis() + "_" + counter + ".jpeg");
        updateModel.setImageURL(uri);
        updateModel.setUploadTime(System.currentTimeMillis());
        imageList.add(updateModel);
        imageAdapter.notifyDataSetChanged();
    }


    @Override
    public void onImageClick(int index) {
        final GalleryModel model = imageList.get(index);
        imageList.remove(index);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompactAlertDialogStyle);
        builder.setMessage("Are you sure to delete this image?")
                .setCancelable(false)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        deleteImage(model);
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }
}

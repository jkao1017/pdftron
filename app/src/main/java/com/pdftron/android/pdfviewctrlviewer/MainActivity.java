package com.pdftron.android.pdfviewctrlviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.common.PDFNetException;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.config.ToolManagerBuilder;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AppUtils;
import com.pdftron.pdf.widget.preset.component.PresetBarComponent;
import com.pdftron.pdf.widget.preset.component.PresetBarViewModel;
import com.pdftron.pdf.widget.preset.component.view.PresetBarView;
import com.pdftron.pdf.widget.preset.signature.SignatureViewModel;
import com.pdftron.pdf.widget.toolbar.ToolManagerViewModel;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarComponent;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarViewModel;
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars;
import com.pdftron.pdf.widget.toolbar.component.view.AnnotationToolbarView;
import com.pdftron.sdf.SDFDoc;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final int PDF_REQ_CODE = 1;
    private PDFViewCtrl mPdfViewCtrl;
    private PDFDoc mPdfDoc;
    private ToolManager mToolManager;
    private AnnotationToolbarComponent mAnnotationToolbarComponent;
    private PresetBarComponent mPresetBarComponent;
    private FrameLayout mToolbarContainer;
    private FrameLayout mPresetContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        mPdfViewCtrl = findViewById(R.id.pdfviewctrl);
        mToolbarContainer = findViewById(R.id.annotation_toolbar_container);
        mPresetContainer = findViewById(R.id.preset_container);
        setupToolManager();
        setupAnnotationToolbar();
        try {
            AppUtils.setupPDFViewCtrl(mPdfViewCtrl);


        } catch (PDFNetException e) {
            Log.e(TAG, "Error setting up PDFViewCtrl");
        }
    }

    /**
     * Helper method to set up and initialize the ToolManager.
     */
    public void setupToolManager() {
        mToolManager = ToolManagerBuilder.from()
                .build(this, mPdfViewCtrl);
    }

    /**
     * Helper method to set up and initialize the AnnotationToolbar.
     */
    public void setupAnnotationToolbar() {
        ToolManagerViewModel toolManagerViewModel = ViewModelProviders.of(this).get(ToolManagerViewModel.class);
        toolManagerViewModel.setToolManager(mToolManager);
        SignatureViewModel signatureViewModel = ViewModelProviders.of(this).get(SignatureViewModel.class);
        PresetBarViewModel presetViewModel = ViewModelProviders.of(this).get(PresetBarViewModel.class);
        AnnotationToolbarViewModel annotationToolbarViewModel = ViewModelProviders.of(this).get(AnnotationToolbarViewModel.class);

        // Create our UI components for the annotation toolbar annd preset bar
        mAnnotationToolbarComponent = new AnnotationToolbarComponent(
                this,
                annotationToolbarViewModel,
                presetViewModel,
                toolManagerViewModel,
                new AnnotationToolbarView(mToolbarContainer)
        );

        mPresetBarComponent = new PresetBarComponent(
                this,
                getSupportFragmentManager(),
                presetViewModel,
                toolManagerViewModel,
                signatureViewModel,
                new PresetBarView(mPresetContainer)
        );

        // Create our custom toolbar and pass it to the annotation toolbar UI component
        mAnnotationToolbarComponent.inflateWithBuilder(
                AnnotationToolbarBuilder.withTag("Custom Toolbar")
                        .addToolButton(ToolbarButtonType.SQUARE, DefaultToolbars.ButtonId.SQUARE.value())
                        .addToolButton(ToolbarButtonType.INK, DefaultToolbars.ButtonId.INK.value())
                        .addToolButton(ToolbarButtonType.FREE_HIGHLIGHT, DefaultToolbars.ButtonId.FREE_HIGHLIGHT.value())
                        .addToolButton(ToolbarButtonType.ERASER, DefaultToolbars.ButtonId.ERASER.value())
                        .addToolStickyButton(ToolbarButtonType.UNDO, DefaultToolbars.ButtonId.UNDO.value())
                        .addToolStickyButton(ToolbarButtonType.REDO, DefaultToolbars.ButtonId.REDO.value())
                        .addCustomButton(R.string.favorites, R.drawable.annotation_note_icon_star_fill,105)
                        .addCustomButton(R.string.file_attachments,R.drawable.annotation_note_icon_checkmark_fill,106)
        );

        mAnnotationToolbarComponent.addButtonClickListener(new AnnotationToolbarComponent.AnnotationButtonClickListener(){

            @Override
            public boolean onInterceptItemClick(@Nullable ToolbarItem toolbarItem, MenuItem item) {

                //String downloadFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                if (item.getItemId() == 105) {
                    File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(downloadFolder, "Final.pdf");
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    Uri downloadFolderPath = Uri.fromFile(file);
                    Toast.makeText(MainActivity.this, "Downloaded", Toast.LENGTH_SHORT).show();
                    try {
                        saveDocument(mPdfViewCtrl, downloadFolderPath,SDFDoc.SaveMode.LINEARIZED);
                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                    return true;
                }
                if (item.getItemId() == 106){
                    Intent intent = new Intent();
                    intent.setType("application/pdf");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent,"Select PDF"), PDF_REQ_CODE);




                }
                return false;

            }




            @Override
            public void onPreItemClick(@Nullable ToolbarItem toolbarItem, MenuItem item) {

            }

            @Override
            public void onPostItemClick(@Nullable ToolbarItem toolbarItem, @NonNull MenuItem item) {

            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PDF_REQ_CODE && resultCode == RESULT_OK && null != data) {
            Uri selectedPdf = data.getData();

            if (selectedPdf.getLastPathSegment().endsWith("pdf")) {
                try {
                    viewFromResource(selectedPdf);
                } catch (PDFNetException | FileNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, "Invalid file type", Toast.LENGTH_SHORT).show();
            }
        }


    }
    void saveDocument(final PDFViewCtrl pdfViewCtrl, final Uri uri, final SDFDoc.SaveMode saveModes) throws Exception {
        pdfViewCtrl.docLock(true, new PDFViewCtrl.LockRunnable() {
            @Override
            public void run() throws Exception {
                SecondaryFileFilter filter = null;
                try {
                    filter = new SecondaryFileFilter(pdfViewCtrl.getContext(), uri);
                    PDFDoc pdfDoc = pdfViewCtrl.getDoc();
                    pdfDoc.save(filter, saveModes);
                } finally {
                    if (filter != null) {
                        filter.close();
                    }
                }
            }
        });
    }

    public void viewFromResource(Uri filePath) throws PDFNetException, FileNotFoundException {

        //mPdfDoc = new PDFDoc(filePath)
        mPdfDoc = mPdfViewCtrl.openPDFUri(filePath,null);
       // mPdfViewCtrl.setDoc(mPdfDoc);
        // Alternatively, you can open the document using Uri:
        // Uri fileUri = Uri.fromFile(file);
        // mPdfDoc = mPdfViewCtrl.openPDFUri(fileUri, null);
    }

    /**
     * We need to clean up and handle PDFViewCtrl based on Android lifecycle callback.
     */

    @Override
    protected void onPause() {
        super.onPause();
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.pause();
            mPdfViewCtrl.purgeMemory();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.resume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.destroy();
            mPdfViewCtrl = null;
        }

        if (mPdfDoc != null) {
            try {
                mPdfDoc.close();
            } catch (Exception e) {
                // handle exception
            } finally {
                mPdfDoc = null;
            }
        }
    }
}

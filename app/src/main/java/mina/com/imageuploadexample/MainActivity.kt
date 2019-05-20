package mina.com.imageuploadexample

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.jakewharton.rxrelay2.BehaviorRelay
import com.miguelbcr.ui.rx_paparazzo2.RxPaparazzo
import durdinapps.rxfirebase2.RxFirebaseStorage
import id.zelory.compressor.Compressor
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private val imageUrlRelay: BehaviorRelay<String> = BehaviorRelay.createDefault("default")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView.setOnClickListener {
            disposable.add(RxPaparazzo.single(this)
                .usingGallery()
                .filter { it.resultCode() == Activity.RESULT_OK }
                .map { response -> response.data() }
                .map {
                    progress.visibility = View.VISIBLE
                    imageView.setImageBitmap(BitmapFactory.decodeFile(it.file.absolutePath))
                    it.file
                }.observeOn(Schedulers.io())
                .map { Compressor(this).compressToFile(it)}
                .concatMap { file ->
                    val ref = FirebaseStorage.getInstance()
                        .reference.child("images/${System.currentTimeMillis().toString() + "_" + file.name}")
                    RxFirebaseStorage
                        .putFile(ref, Uri.fromFile(file))
                        .observeOn(Schedulers.io())
                        .flatMap {
                            Single.create<File> {
                                ref.downloadUrl.addOnSuccessListener { uri ->
                                    imageUrlRelay.accept(uri.toString())
                                    it.onSuccess(file)
                                }
                            }
                        }.toObservable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { progress.visibility = View.GONE }
                .subscribe({}, { processError(it) })
            )

        }

        disposable.add(imageUrlRelay
            .hide()
            .filter { it != "default" }
            .subscribe { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() })
    }

    private fun processError(throwable: Throwable?) {
        Log.d("mina-Error", throwable?.localizedMessage)
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}

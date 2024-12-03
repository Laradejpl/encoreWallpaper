package com.tektak.encorewallpaper

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class ImageAdapter(private val context: Context, private val images: IntArray) : BaseAdapter() {

    private var selectedPosition = -1

    override fun getCount(): Int {
        return images.size
    }

    override fun getItem(position: Int): Any {
        return images[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView
        if (convertView == null) {
            imageView = ImageView(context)
            imageView.layoutParams = ViewGroup.LayoutParams(500, 500)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            imageView = convertView as ImageView
        }
        imageView.setImageResource(images[position])

        // Mettre en évidence l'image sélectionnée
        if (position == selectedPosition) {
            imageView.setBackgroundResource(R.drawable.image_selected_background)
        } else {
            imageView.setBackgroundResource(0)
        }

        return imageView
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }
}
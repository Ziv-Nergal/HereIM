package adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder {

    private String mViewHolderId;

    BaseViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    abstract void bindView(T objectToBind);

    void setViewHolderId(String viewHolderId) {
        this.mViewHolderId = viewHolderId;
    }

    String getViewHolderId() {
        return mViewHolderId;
    }
}

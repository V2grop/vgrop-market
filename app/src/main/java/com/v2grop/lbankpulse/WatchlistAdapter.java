package com.v2grop.lbankpulse;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** Native drag sorting, edge autoscroll and equivalent TalkBack actions. */
final class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.Holder> {
    private final List<MarketItem> items;
    private final MarketAdapter.Renderer renderer;
    private final java.util.function.Consumer<List<MarketItem>> save;
    final ItemTouchHelper touch;
    private boolean dragging;

    WatchlistAdapter(List<MarketItem> items,MarketAdapter.Renderer renderer,
                     java.util.function.Consumer<List<MarketItem>> save) {
        this.items=new ArrayList<>(items);this.renderer=renderer;this.save=save;
        touch=new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP|ItemTouchHelper.DOWN,0) {
            @Override public boolean isLongPressDragEnabled(){return true;}
            @Override public boolean isItemViewSwipeEnabled(){return false;}
            @Override public boolean onMove(@NonNull RecyclerView r,@NonNull RecyclerView.ViewHolder from,@NonNull RecyclerView.ViewHolder to){
                return move(from.getBindingAdapterPosition(),to.getBindingAdapterPosition());
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder h,int direction){}
            @Override public void onSelectedChanged(RecyclerView.ViewHolder h,int state){
                dragging=state==ItemTouchHelper.ACTION_STATE_DRAG;
                if(h!=null && dragging){
                    h.itemView.setAlpha(.88f);h.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    h.itemView.getParent().requestDisallowInterceptTouchEvent(true);
                }
                super.onSelectedChanged(h,state);
            }
            @Override public void clearView(@NonNull RecyclerView r,@NonNull RecyclerView.ViewHolder h){
                super.clearView(r,h);h.itemView.setAlpha(1f);dragging=false;
                r.getParent().requestDisallowInterceptTouchEvent(false);
                // Position-dependent accessibility actions must follow the new order.
                notifyItemRangeChanged(0,getItemCount());
            }
        });
    }
    boolean move(int from,int to){
        if(from<0||to<0||from>=items.size()||to>=items.size()||from==to)return false;
        FavoriteOrder.move(items,from,to);save.accept(new ArrayList<>(items));notifyItemMoved(from,to);return true;
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final LinearLayout root;
        Holder(LinearLayout root){super(root);this.root=root;}
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){
        LinearLayout root=new LinearLayout(parent.getContext());root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return new Holder(root);
    }
    @Override public void onBindViewHolder(@NonNull Holder h,int position){
        h.root.removeAllViews();renderer.render(items.get(position),h.root);
        View card=h.root.getChildAt(0);
        card.setOnLongClickListener(v->{if(h.getBindingAdapterPosition()==RecyclerView.NO_POSITION)return false;touch.startDrag(h);return true;});
        ViewCompat.addAccessibilityAction(card,"انتقال به جایگاه بالاتر",(v,args)->accessibleMove(h,-1));
        ViewCompat.addAccessibilityAction(card,"انتقال به جایگاه پایین‌تر",(v,args)->accessibleMove(h,1));
    }
    private boolean accessibleMove(Holder h,int delta){
        int from=h.getBindingAdapterPosition();boolean changed=move(from,from+delta);
        if(changed){notifyItemRangeChanged(0,getItemCount());h.root.announceForAccessibility("ترتیب ذخیره شد");}return changed;
    }
    @Override public int getItemCount(){return items.size();}
}

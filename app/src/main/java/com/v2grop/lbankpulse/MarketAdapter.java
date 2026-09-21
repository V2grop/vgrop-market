package com.v2grop.lbankpulse;
import android.view.ViewGroup;import android.widget.LinearLayout;
import androidx.annotation.NonNull;import androidx.recyclerview.widget.DiffUtil;import androidx.recyclerview.widget.ListAdapter;import androidx.recyclerview.widget.RecyclerView;
/** Virtualized catalog; only visible rows create views. Stable spot/perp identity. */
public final class MarketAdapter extends ListAdapter<MarketItem,MarketAdapter.Holder> {
 public interface Renderer {void render(MarketItem item,LinearLayout parent);}
 private final Renderer renderer;
 public MarketAdapter(Renderer renderer){super(new DiffUtil.ItemCallback<MarketItem>(){
  public boolean areItemsTheSame(@NonNull MarketItem a,@NonNull MarketItem b){return a.symbol.equals(b.symbol)&&a.marketType.equals(b.marketType);}
  public boolean areContentsTheSame(@NonNull MarketItem a,@NonNull MarketItem b){return a.display.equals(b.display)&&a.marketType.equals(b.marketType);}
 });this.renderer=renderer;}
 public static final class Holder extends RecyclerView.ViewHolder {final LinearLayout root;Holder(LinearLayout v){super(v);root=v;}}
 @NonNull public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){LinearLayout v=new LinearLayout(parent.getContext());v.setOrientation(LinearLayout.VERTICAL);v.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return new Holder(v);}
 public void onBindViewHolder(@NonNull Holder h,int position){h.root.removeAllViews();renderer.render(getItem(position),h.root);}
}

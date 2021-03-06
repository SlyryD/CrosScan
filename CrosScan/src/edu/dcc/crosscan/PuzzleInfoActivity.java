package edu.dcc.crosscan;

import java.text.NumberFormat;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import edu.dcc.db.CrosswordDatabase;
import edu.dcc.game.CrosswordGame;

public class PuzzleInfoActivity extends ListActivity {

	private CrosswordDatabase mDatabase;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_puzzle_info);
		setTitle(R.string.puzzle_info);

		mDatabase = new CrosswordDatabase(getApplicationContext());

		CrosswordGame game = mDatabase.getCrossword(getIntent().getLongExtra(
				Constants.EXTRA_CROSSWORD_ID, 1));

		InfoItem[] items = new InfoItem[] {
				new InfoItem("Title", game.getTitle()),
				new InfoItem("Date Created", Long.toString(game.getCreated())),
				new InfoItem("Last Played", Long.toString(game.getLastPlayed())),
				new InfoItem("Time Played", new GameTimeFormat().format(game
						.getTime())),
				new InfoItem("Complete", NumberFormat.getPercentInstance()
						.format(game.getCompletion())),
				new InfoItem("Size", Integer.toString(game.getPuzzle()
						.getHeight())
						+ "x"
						+ Integer.toString(game.getPuzzle().getWidth())) };

		InfoItemAdapter adapter = new InfoItemAdapter(this,
				R.layout.puzzle_info_item, items);

		ListView listView = getListView();
		listView.setAdapter(adapter);
	}

	public class InfoItem {
		public String header;
		public String footer;

		public InfoItem(final String header, final String footer) {
			this.header = header;
			this.footer = footer;
		}

		/**
		 * @return the header
		 */
		public final String getHeader() {
			return header;
		}

		/**
		 * @return the footer
		 */
		public final String getFooter() {
			return footer;
		}

	}

	public class InfoItemAdapter extends ArrayAdapter<InfoItem> {
		private InfoItem[] items;

		public InfoItemAdapter(final Context context, final int textViewResourceId,
				final InfoItem[] items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public final View getView(final int position, final View convertView, final ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.puzzle_info_item, null);
			}

			InfoItem item = items[position];
			if (item != null) {
				TextView username = (TextView) view.findViewById(R.id.header);
				TextView email = (TextView) view.findViewById(R.id.footer);

				if (username != null) {
					username.setText(item.getHeader());
				}

				if (email != null) {
					email.setText(item.getFooter());
				}
			}
			return view;
		}
	}
}

/*
This file is part of Keepass2Android, Copyright 2013 Philipp Crocoll. 

  Keepass2Android is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 2 of the License, or
  (at your option) any later version.

  Keepass2Android is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Keepass2Android.  If not, see <http://www.gnu.org/licenses/>.
  */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Util;
using Android.Views;
using Android.Widget;
using KeePassLib.Security;

namespace keepass2android.view
{
	public class EntryEditSection : RelativeLayout 
	{
		public event EventHandler ContentChanged;

		public EntryEditSection (IntPtr javaReference, JniHandleOwnership transfer)
			: base(javaReference, transfer)
		{
			
		}

		public EntryEditSection(Context context, IAttributeSet attrs) :
			base (context, attrs)
		{
			Initialize();
		}

		public EntryEditSection(Context context, IAttributeSet attrs, int defStyle) :
			base (context, attrs, defStyle)
		{
			Initialize();
		}

		private void Initialize()
		{
		}
				
		
		public void setData(String title, ProtectedString value) {
			setText(Resource.Id.title, title);
			setText(Resource.Id.value, value.ReadString());
			
			CheckBox cb = (CheckBox) FindViewById(Resource.Id.protection);
			cb.Checked = value.IsProtected;
			cb.CheckedChange += (sender, e) => {if (ContentChanged != null)
				ContentChanged(this, new EventArgs());
			};
		}

		public ImageButton getDeleteButton()
		{
			return (ImageButton)FindViewById(Resource.Id.delete);
		}
		
		private void setText(int resId, String str)
		{
			if (str != null)
			{
				TextView tv = (TextView)FindViewById(resId);
				tv.Text = str;
				tv.TextChanged += (sender, e) => {
					if (ContentChanged != null)
						ContentChanged(this, new EventArgs());
				};
			}
			
		}
	}
}

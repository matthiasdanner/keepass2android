﻿using System;
using System.Collections.Generic;
using System.Linq;
using Android.Content;
using Android.Content.PM;
using Android.OS;
using Android.Preferences;
using Android.Runtime;
using Android.Service.Autofill;
using Android.Util;
using Android.Views.Autofill;
using Android.Widget;
using keepass2android.services.AutofillBase.model;

namespace keepass2android.services.AutofillBase
{
    public interface IAutofillIntentBuilder
    {
        IntentSender GetAuthIntentSenderForResponse(Context context, string query, bool isManualRequest, bool autoReturnFromQuery);
        IntentSender GetDisableIntentSenderForResponse(Context context, string query, bool isManualRequest, bool isDisable);
        Intent GetRestartAppIntent(Context context);

        int AppIconResource { get; }
    }

    public abstract class AutofillServiceBase: AutofillService
    {
        public AutofillServiceBase()
        {
            
        }

        public AutofillServiceBase(IntPtr javaReference, JniHandleOwnership transfer)
            : base(javaReference, transfer)
        {
        }


        public override void OnFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback)
        {
            bool isManual = (request.Flags & FillRequest.FlagManualRequest) != 0;
            CommonUtil.logd( "onFillRequest " + (isManual ? "manual" : "auto"));
            var structure = request.FillContexts[request.FillContexts.Count - 1].Structure;

            //TODO support package signature verification as soon as this is supported in Keepass storage

            var clientState = request.ClientState;
            CommonUtil.logd( "onFillRequest(): data=" + CommonUtil.BundleToString(clientState));


            cancellationSignal.CancelEvent += (sender, e) => {
                Log.Warn(CommonUtil.Tag, "Cancel autofill not implemented yet.");
            };
            // Parse AutoFill data in Activity
            string query = null;
            var parser = new StructureParser(this, structure);
            try
            {
                query = parser.ParseForFill(isManual);
                
            }
            catch (Java.Lang.SecurityException e)
            {
                Log.Warn(CommonUtil.Tag, "Security exception handling request");
                callback.OnFailure(e.Message);
                return;
            }
            
            AutofillFieldMetadataCollection autofillFields = parser.AutofillFields;
            
            
            var autofillIds = autofillFields.GetAutofillIds();
            if (autofillIds.Length != 0 && CanAutofill(query, isManual))
            {
                var responseBuilder = new FillResponse.Builder();

                var entryDataset = AddEntryDataset(query, parser);
                bool hasEntryDataset = entryDataset != null;
                if (entryDataset != null)
                    responseBuilder.AddDataset(entryDataset);

                AddQueryDataset(query, isManual, autofillIds, responseBuilder, !hasEntryDataset);
                AddDisableDataset(query, autofillIds, responseBuilder, isManual);
                if (PreferenceManager.GetDefaultSharedPreferences(this).GetBoolean(GetString(Resource.String.OfferSaveCredentials_key), true))
                    responseBuilder.SetSaveInfo(new SaveInfo.Builder(parser.AutofillFields.SaveType,
                        parser.AutofillFields.GetAutofillIds()).Build());

                callback.OnSuccess(responseBuilder.Build());
            }
            else
            {
                callback.OnSuccess(null);
            }
        }

        private Dataset AddEntryDataset(string query, StructureParser parser)
        {
            var filledAutofillFieldCollection = GetSuggestedEntry(query);
            if (filledAutofillFieldCollection == null)
                return null;
            int partitionIndex = AutofillHintsHelper.GetPartitionIndex(parser.AutofillFields.FocusedAutofillCanonicalHints.FirstOrDefault());
            FilledAutofillFieldCollection partitionData = AutofillHintsHelper.FilterForPartition(filledAutofillFieldCollection, partitionIndex);

            return AutofillHelper.NewDataset(this, parser.AutofillFields, partitionData, IntentBuilder);
        }

        protected abstract FilledAutofillFieldCollection GetSuggestedEntry(string query);

        private void AddQueryDataset(string query, bool isManual, AutofillId[] autofillIds, FillResponse.Builder responseBuilder, bool autoReturnFromQuery)
        {
            var sender = IntentBuilder.GetAuthIntentSenderForResponse(this, query, isManual, autoReturnFromQuery);
            RemoteViews presentation = AutofillHelper.NewRemoteViews(PackageName,
                GetString(Resource.String.autofill_sign_in_prompt), AppNames.LauncherIcon);

            var datasetBuilder = new Dataset.Builder(presentation);
            datasetBuilder.SetAuthentication(sender);
            //need to add placeholders so we can directly fill after ChooseActivity
            foreach (var autofillId in autofillIds)
            {
                datasetBuilder.SetValue(autofillId, AutofillValue.ForText("PLACEHOLDER"));
            }

            responseBuilder.AddDataset(datasetBuilder.Build());
        }
        public static string GetDisplayNameForQuery(string str, Context Context)
        {
            string displayName = str;
            try
            {
                string appPrefix = "androidapp://";
                if (str.StartsWith(appPrefix))
                {
                    str = str.Substring(appPrefix.Length);
                    PackageManager pm = Context.PackageManager;
                    ApplicationInfo ai;
                    try
                    {
                        ai = pm.GetApplicationInfo(str, 0);
                    }
                    catch (PackageManager.NameNotFoundException e)
                    {
                        ai = null;
                    }
                    displayName = ai != null ? pm.GetApplicationLabel(ai) : str;
                }
            }
            catch (Exception e)
            {
                Kp2aLog.LogUnexpectedError(e);
            }
           
            return displayName;
        }

        private void AddDisableDataset(string query, AutofillId[] autofillIds, FillResponse.Builder responseBuilder, bool isManual)
        {
            bool isQueryDisabled = IsQueryDisabled(query);
            if (isQueryDisabled && !isManual)
                return;
            bool isForDisable = !isQueryDisabled;
            var sender = IntentBuilder.GetDisableIntentSenderForResponse(this, query, isManual, isForDisable);
            
            RemoteViews presentation = AutofillHelper.NewRemoteViews(PackageName,
                GetString(isForDisable ? Resource.String.autofill_disable : Resource.String.autofill_enable_for, new Java.Lang.Object[] { GetDisplayNameForQuery(query, this)}), Resource.Drawable.ic_menu_close_grey);

            var datasetBuilder = new Dataset.Builder(presentation);
            datasetBuilder.SetAuthentication(sender);

            foreach (var autofillId in autofillIds)
            {
                datasetBuilder.SetValue(autofillId, AutofillValue.ForText("PLACEHOLDER"));
            }

            responseBuilder.AddDataset(datasetBuilder.Build());
        }

        private bool CanAutofill(string query, bool isManual)
        {
            if (query == "androidapp://android" || query == "androidapp://" + this.PackageName)
                return false;
            if (!isManual)
            {
                var isQueryDisabled = IsQueryDisabled(query);
                if (isQueryDisabled)
                    return false;
            }
            return true;
        }

        private bool IsQueryDisabled(string query)
        {
            var prefs = PreferenceManager.GetDefaultSharedPreferences(this);
            var disabledValues = prefs.GetStringSet("AutoFillDisabledQueries", new List<string>());

            bool isQueryDisabled = disabledValues.Contains(query);
            return isQueryDisabled;
        }

        public override void OnSaveRequest(SaveRequest request, SaveCallback callback)
        {

            var structure = request.FillContexts?.LastOrDefault()?.Structure;
            if (structure == null)
            {
                return;
            }

            var parser = new StructureParser(this, structure);
            string query = parser.ParseForSave();
            try
            {
                HandleSaveRequest(parser, query);
                callback.OnSuccess();
            }
            catch (Exception e)
            {
                callback.OnFailure(e.Message);   
            }
            
        }

        protected abstract void HandleSaveRequest(StructureParser parser, string query);


        public override void OnConnected()
        {
            CommonUtil.logd( "onConnected");
        }

        public override void OnDisconnected()
        {
            CommonUtil.logd( "onDisconnected");
        }

        public abstract IAutofillIntentBuilder IntentBuilder{get;}
    }
}

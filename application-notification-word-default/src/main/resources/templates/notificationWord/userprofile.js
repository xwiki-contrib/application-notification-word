/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
define('wordsNotificationsTranslationKeys', {
  prefix: 'wordsNotification.settings.try.',
  keys: [
    "analysisStart",
    "analysisError",
    "matchFound"
  ]
});
require(['jquery', 'xwiki-meta', 'xwiki-l10n!wordsNotificationsTranslationKeys'], function($, xm, l10n) {
  var testPattern = function() {
    let postUrl = new XWiki.Document(xm.documentReference).getURL('get');
    let textInput = $('#textToAnalyze').val();
    let params = {
      'action': 'testpattern',
      'wordsquery': $('#wordsquery').val(),
      'textToAnalyze': textInput,
      'form_token': xm.form_token
    };
    var notification = new XWiki.widgets.Notification(l10n['analysisStart'],'inprogress');
    // We don't want to manipulate the hidden class: it's just used for first display.
    $('.no-matching-result').removeClass('hidden');
    $('.matching-results').removeClass('hidden');
    $('.no-matching-result').hide();
    $('.matching-results').hide();
    $.post(postUrl, params).done(function (data) {
      console.log(data);
      let index = 0;
      if (data.length > 0) {
        let result = l10n.get('matchFound', data.length);
        result += "<div class=\"text-result\">";
        for (let i = 0; i < data.length; i++) {
          let region = data[i];
          result += textInput.substring(index, region.start);
          result += "<span class=\"matching-region\">";
          result += textInput.substring(region.start, region.end);
          result += "</span>";
          index = region.end;
        }
        if (index < textInput.length) {
          result += textInput.substring(index);
        }
        result += "</div>";
        $('.matching-results').html(result);
        $('.matching-results').show();
      } else {
        $('.no-matching-result').show();
      }
      notification.hide();
    }).fail(function (data) {
      notification.replace(new XWiki.widgets.Notification(l10n['analysisError'],'error'));
    });
  }

  var init = function () {
    $('#tryButton').on('click', testPattern);
  }

  $(document).on('xwiki:dom:updated', init);
  return XWiki.domIsLoaded && init();
});
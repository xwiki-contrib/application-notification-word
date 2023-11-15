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
require(['jquery', 'xwiki-meta'], function($, xm) {
  var testPattern = function() {
    let postUrl = new XWiki.Document(xm.documentReference).getURL('get');
    let textInput = $('#textToAnalyze').val();
    let params = {
      'action': 'testpattern',
      'wordsquery': $('#wordsquery').val(),
      'textToAnalyze': textInput,
      'form_token': xm.form_token
    };
    var notification = new XWiki.widgets.Notification('Perform pattern analysis...','inprogress');
    $.post(postUrl, params).done(function (data) {
      console.log(data);
      let index = 0;
      let result = "";
      if (data.length > 0) {
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
      } else {
        // FIXME: This should not be done like that.
        result = "<span class=\"no-match\">No match found.</span>";
      }
      $('#matchingResult').html(result);
      notification.hide();
    }).fail(function (data) {
      notification.replace(new XWiki.widgets.Notification('Error while testing the pattern.','error'));
    });
  }

  var init = function () {
    $('#tryButton').on('click', testPattern);
  }

  $(document).on('xwiki:dom:updated', init);
  return XWiki.domIsLoaded && init();
});
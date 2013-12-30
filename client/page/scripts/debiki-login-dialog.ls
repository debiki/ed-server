/* Shows a login dialog.
 * Copyright (C) 2012-2013 Kaj Magnus Lindberg (born 1979)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


d = i: debiki.internal, u: debiki.v0.util
$ = d.i.$;



d.i.showLoginSubmitDialog = !(anyMode) ->
  showLoginDialog (anyMode || 'SubmitGeneric')



!function showLoginDialog(mode)

  doingWhatClass = switch mode
  | 'SubmitGeneric' => 'dw-login-to-submit'
  | 'SubmitComment' => 'dw-login-to-post-comment'
  | _ => 'dw-login-to-login'
  $('body').addClass(doingWhatClass)

  dialog = loginDialogHtml()
  dialog.dialog d.i.newModalDialogSettings({ width: 413 })

  dialog.find('#dw-lgi-guest').click ->
    d.i.showGuestLoginDialog(loginAndContinue)
    false

  dialog.find('#dw-lgi-pswd').click ->
    d.i.showPasswordLoginDialog(loginAndContinue)
    false

  dialog.find('#dw-lgi-more').click ->
    d.i.showLoginOpenId()
    false

  dialog.find('.dw-fi-cancel').click ->
    close()
    false

  dialog.find('a#dw-lgi-google').click ->
    loginGoogleYahoo("https://www.google.com/accounts/o8/id")

  dialog.find('a#dw-lgi-yahoo').click ->
    loginGoogleYahoo("http://me.yahoo.com/")

  dialog.find('a#dw-lgi-facebook').click ->
    openSecureSocialLoginWindow('facebook')

  /**
   * Logs in at Google or Yahoo by submitting an OpenID login form in a popup.
   */
  function loginGoogleYahoo(openidIdentifier)
    form = $("""
      <form action="#{d.i.serverOrigin}/-/api/login-openid" method="POST">
        <input type="text" name="openid_identifier" value="#openidIdentifier">
      </form>
      """)
    d.i.createOpenIdLoginPopup(form)
    form.submit()
    false

  function openSecureSocialLoginWindow(provider)
    d.i.createLoginPopup("#{d.i.serverOrigin}/-/login-securesocial-popup/#provider")

  !function loginAndContinue(data)
    d.i.Me.fireLogin()
    # Show response dialog, and continue with whatever caused
    # the login to happen.
    # {{{ If the login happens because the user submits a reply,
    # then, if the reply is submitted (from within
    # continueAnySubmission) before the dialog is closed, then,
    # when the browser moves the viewport to focus the new reply,
    # the welcome dialog might no longer be visible in the viewport.
    # But the viewport will still be dimmed, because the welcome
    # dialog is modal. So don't continueAnySubmission until
    # the user has closed the response dialog. }}}
    close()
    showLoggedInDialog(d.i.continueAnySubmission)

  !function close
    dialog.dialog('close')
    $('body').removeClass(doingWhatClass)

  # Preload OpenID resources, in case user clicks OpenID login button.
  d.i.loadOpenIdResources()

  dialog.dialog 'open'



!function showLoggedInDialog(opt_continue)
  html = $('''
      <p>You have been logged in, welcome <span id="dw-lgi-name"></span></p>
    ''')
  html.find('#dw-lgi-name').text(d.i.Me.getName!)
  html.dialog $.extend({}, d.i.jQueryDialogNoClose,
      title: 'Welcome'
      autoOpen: true
      buttons: [
        text: 'OK'
        id: 'dw-dlg-rsp-ok'
        click: !->
          # Remove the dialog, so the OK button id can be reused
          # — then it's easier to write automatic tests.
          $(this).dialog('destroy').remove()
          if opt_continue
            opt_continue()
      ])



function loginDialogHtml
  $('''
    <div class="dw-fs" title="Who are you?" id="dw-lgi">
      <a id="dw-lgi-guest" class="btn btn-default" tabindex="101">Login as Guest</a>
      <a id="dw-lgi-pswd" class="btn btn-default" tabindex="102">Login with Email and Password</a>

      <p id="dw-lgi-or-login-using">
        Or login<span class="dw-login-to-post-comment">, and post your comment,</span>
        using your account (if any) at:</p>
      <div id="dw-lgi-other-sites">
        <a id="dw-lgi-google" class="btn btn-default" tabindex="103">
          <span class="icon-google-plus"></span>Google
        </a>
        <a id="dw-lgi-facebook" class="btn btn-default" tabindex="104">
          <span class="icon-facebook"></span>
          Facebook
        </a>
        <a id="dw-lgi-yahoo" class="btn btn-default" tabindex="105">
          <span class="icon-yahoo"></span>
          Yahoo!
        </a>
      </div>

      <a id="dw-lgi-more" class="btn btn-default" tabindex="106">More options...</a>

      <input class="btn btn-default dw-fi-cancel" type="button" value="Cancel" tabindex="107">
    </div>
    ''')



$(!->
  $('#dw-a-login').click showLoginDialog)



# vim: fdm=marker et ts=2 sw=2 fo=tcqwn list

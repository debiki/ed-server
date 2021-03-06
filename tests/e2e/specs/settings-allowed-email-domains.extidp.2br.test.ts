/// <reference path="../test-types.ts"/>

import * as _ from 'lodash';
import assert = require('assert');
import server = require('../utils/server');
import utils = require('../utils/utils');
import { buildSite } from '../utils/site-builder';
import { TyE2eTestBrowser } from '../utils/pages-for';
import settings = require('../utils/settings');
import logAndDie = require('../utils/log-and-die');
import c = require('../test-constants');



let richBrowserA;
let richBrowserB;
let owen: Member;
let owensBrowser: TyE2eTestBrowser;
let strangersBrowser: TyE2eTestBrowser;

let siteIdAddress: IdAddress;
let siteId;

let forum: EmptyTestForum;

const evilOrgDomain = 'evil.org';
const evilOrgUserOne =
    { emailAddress: 'e2e-test-one@evil.org', username: "Evl_1", password: "I shall p4ss" };

const okayOrgDomain = 'okay.org';
const okayOrgUserOne =
    { emailAddress: 'e2e-test-one@okay.org', username: "Ok_1", password: "Cats c4n climb" };

const otherOrgUser =
    { emailAddress: 'e2e-test-one@other.org', username: "Otr_1", password: "1 cat is 1 pet" };

const notOkayOrgDomain = 'not.' + okayOrgDomain;
const notOkayOrgUser =
    { emailAddress: 'e2e-test-a@' + notOkayOrgDomain, username: "NOk", password: "1 pet is 1 cat" };


describe("settings-allowed-email-domains.extidp.2br  TyT5AKRD04", () => {

  it("import a site", () => {
    const builder = buildSite();
    forum = builder.addEmptyForum({
      title: "Allowed Email Domains E2E Test",
      members: undefined, // default = everyone
    });
    assert(builder.getSite() === forum.siteData);
    siteIdAddress = server.importSiteData(forum.siteData);
    siteId = siteIdAddress.id;
  });

  it("initialize people", () => {
    richBrowserA = new TyE2eTestBrowser(browserA);
    richBrowserB = new TyE2eTestBrowser(browserB);

    owen = forum.members.owen;
    owensBrowser = richBrowserA;

    strangersBrowser = richBrowserB;
  });

  it("Owen logs in to admin area, ... ", () => {
    owensBrowser.adminArea.goToLoginSettings(siteIdAddress.origin);
    owensBrowser.loginDialog.loginWithPassword(owen);
  });


  // ----- Blocklist

  it("Owen blocklists the domains 'very.bad.com' and 'evil.org'", () => {
    owensBrowser.adminArea.settings.login.setEmailDomainBlocklist(
      'very.bad.com\n' +
      'oh.so.not.not\n' +
      '# A comment and blank line and a whitespace line\n' +
      '\n' +
      '   \n' +
      '   ' + evilOrgDomain + '  ');
    owensBrowser.adminArea.settings.clickSaveAll();
  });

  it("A stranger, evilOrgUserOne, arrives", () => {
    strangersBrowser.go(siteIdAddress.origin);
  });

  it("... attempts to sign up with a blocklisted domains", () => {
    strangersBrowser.complex.signUpAsMemberViaTopbar(evilOrgUserOne);
  });

  it("... and gets a bad domain error message 1", () => {
    strangersBrowser.serverErrorDialog.waitForBadEmailDomainError();
  });

  it("... closes the error message dialog", () => {
    strangersBrowser.serverErrorDialog.close();
  });

  it("Owen clears the blocklist", () => {
    owensBrowser.adminArea.settings.login.setEmailDomainBlocklist('');
    owensBrowser.adminArea.settings.clickSaveAll();  // BUG won't reappear  !
  });

  it("... Now evilOrgUserOne can sign up", () => {
    // --- Currently needed because Chrome won't clear email input field  [E2EBUG]
    strangersBrowser.loginDialog.clickCancel();
    strangersBrowser.topbar.clickSignUp();
    // -----------------------------------------------------------------
    strangersBrowser.loginDialog.createPasswordAccount(evilOrgUserOne);
  });

  it("... and gets an email addr verif email", () => {
    server.getLastVerifyEmailAddressLinkEmailedTo(
        siteIdAddress.id, evilOrgUserOne.emailAddress, strangersBrowser);
  });


  // ----- Whithelist

  it("Owen adds an email domain whitelist, good.org", () => {
    owensBrowser.adminArea.settings.login.setEmailDomainWhitelist(
      'okay.domain.com\n' +
      '# Another comment and blank line and a whitespace line\n' +
      '\n' +
      '   \n' +
      '   ' + okayOrgDomain + '  ');
    owensBrowser.adminArea.settings.clickSaveAll();
  });

  it("A stranger attempts to sign up with a non white listed domain", () => {
    strangersBrowser.refresh();
    strangersBrowser.complex.signUpAsMemberViaTopbar(otherOrgUser);
  });

  it("... and gets a bad domain error message 2", () => {
    strangersBrowser.serverErrorDialog.waitForBadEmailDomainError();
  });

  it("Another user *can* sign up with an email addr on the white listed domain", () => {
    strangersBrowser.refresh();
    strangersBrowser.complex.signUpAsMemberViaTopbar(okayOrgUserOne);
  });

  it("... and gets an email addr verif email", () => {
    server.getLastVerifyEmailAddressLinkEmailedTo(
        siteIdAddress.id, okayOrgUserOne.emailAddress, strangersBrowser);
  });


  // ----- Allowlist with blocklisted sub domains

  it("Owen black lists a sub domain of the whitelist: " + notOkayOrgDomain, () => {
    owensBrowser.adminArea.settings.login.setEmailDomainBlocklist(notOkayOrgDomain);
    owensBrowser.adminArea.settings.clickSaveAll();
  });

  it("A stranger attempts to sign up via this bad sub domain", () => {
    strangersBrowser.refresh();
    strangersBrowser.complex.signUpAsMemberViaTopbar(notOkayOrgUser);
  });

  it("... and gets a bad domain error message 3", () => {
    strangersBrowser.serverErrorDialog.waitForBadEmailDomainError();
  });

  it("And signing up with other domains", () => {
    strangersBrowser.refresh();
    strangersBrowser.complex.signUpAsMemberViaTopbar(otherOrgUser);
  });

  it("... still doesn't work", () => {
    strangersBrowser.serverErrorDialog.waitForBadEmailDomainError();
  });


  // ----- OpenAuth rejected (not white listed)

  if (settings.include3rdPartyDependentTests && settings.gmailEmail) {
    it("A Gmail user arrives", () => {
      strangersBrowser.refresh();
      strangersBrowser.topbar.clickSignUp();
      strangersBrowser.loginDialog.loginWithGmail(
          { email: settings.gmailEmail, password: settings.gmailPassword },
          false, { stayInPopup: true });
    });

    it("... but gmail.com isn't white listed, signup rejected", () => {
        strangersBrowser.waitUntilPageHtmlSourceMatches_1('TyEBADEMLDMN_-OAUTH_');
    });
  }

});


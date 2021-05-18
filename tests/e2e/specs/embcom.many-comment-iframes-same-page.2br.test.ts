/// <reference path="../test-types.ts"/>

import * as _ from 'lodash';
import assert = require('../utils/ty-assert');
import fs = require('fs');
import server = require('../utils/server');
import utils = require('../utils/utils');
import { buildSite } from '../utils/site-builder';
import { TyE2eTestBrowser, TyAllE2eTestBrowsers } from '../utils/pages-for';
import settings = require('../utils/settings');
import lad = require('../utils/log-and-die');
import c = require('../test-constants');


let everyonesBrowsers: TyAllE2eTestBrowsers;
let brA: TyE2eTestBrowser;
let brB: TyE2eTestBrowser;
let owen: Member;
let owen_brA: TyE2eTestBrowser;
let mons: Member;
let mons_brA: TyE2eTestBrowser;
let modya: Member;
let modya_brA: TyE2eTestBrowser;
let corax: Member;
let corax_brA: TyE2eTestBrowser;
let regina: Member;
let regina_brB: TyE2eTestBrowser;
let maria: Member;
let maria_brB: TyE2eTestBrowser;
let memah: Member;
let memah_brB: TyE2eTestBrowser;
let michael: Member;
let michael_brB: TyE2eTestBrowser;
let mallory: Member;
let mallory_brB: TyE2eTestBrowser;
let stranger_brB: TyE2eTestBrowser;

let site: IdAddress;
let forum: TwoCatsTestForum;
const localHostname = 'comments-for-e2e-test-manyifr-localhost-8080';
const embeddingOrigin = 'http://e2e-test-manyifr.localhost:8080';

const embPage404SlashSlug = '/many-embcom-iframes-404.html';
const embPage404FilePath = 'target' + embPage404SlashSlug;


describe(`embcom.many-comment-iframes-same-page.2br  TyTE2E50RMF24S`, () => {

  it(`construct site`, () => {
    const builder = buildSite();
    forum = builder.addTwoCatsForum({
      title: "Many Comment Iframes",
      members: undefined, // default = everyone
        // ['mons', 'modya', 'regina', 'corax', 'memah', 'maria', 'michael', 'mallory']
    });

    builder.getSite().meta.localHostname = localHostname;
    builder.getSite().settings.allowEmbeddingFrom = embeddingOrigin;


    everyonesBrowsers = new TyE2eTestBrowser(allWdioBrowsers);
    brA = new TyE2eTestBrowser(wdioBrowserA);
    brB = new TyE2eTestBrowser(wdioBrowserB);

    owen = forum.members.owen;
    owen_brA = brA;
    mons = forum.members.mons;
    mons_brA = brA;
    modya = forum.members.modya;
    modya_brA = brA;
    corax = forum.members.corax;
    corax_brA = brA;

    regina = forum.members.regina;
    regina_brB = brB;
    maria = forum.members.maria;
    maria_brB = brB;
    memah = forum.members.memah;
    memah_brB = brB;
    michael = forum.members.michael;
    michael_brB = brB;
    mallory = forum.members.mallory;
    mallory_brB = brB;
    stranger_brB = brB;

    assert.refEq(builder.getSite(), forum.siteData);
  });

  it(`import site`, () => {
    site = server.importSiteData(forum.siteData);
    server.skipRateLimits(site.id);
  });

  /*
  it(`Owen logs in to admin area, ... `, () => {
    owen_brA.adminArea.goToUsersEnabled(site.origin);
    owen_brA.loginDialog.loginWithPassword(owen);
  }); */



  it(`Creates an embedding page`, () => {
    const dir = 'target';
    fs.writeFileSync(embPage404FilePath, makeHtml('404', '#404'));
    //fs.writeFileSync(`${dir}/page-b-slug.html`, makeHtml('bbb', '#040'));

    function makeHtml(pageName: string, bgColor: string): string {
      return utils.makeManyEmbeddedCommentsHtml({
              pageName, discussionIds: ['111', '222'], localHostname, bgColor});
    }
  });

  it("Maria opens embedding page aaa", () => {
    maria_brB.go(embeddingOrigin + embPage404SlashSlug);
  });

  it("... logs in", () => {
    //maria_brB.complex.loginIfNeededViaMetabar(maria);
  });

});


"use strict";

// --------
// WARNING:
// --------

// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE!
// DO NOT USE IN PRODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS
// NOT THE RECOMMENDED WAY TO INTERFACE WITH CORDA NODES! HOWEVER, FOR THIS
// PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES
// VIA THE NODE'S RPC INTERFACE. IN THE COMING WEEKS WE'LL WRITE A TUTORIAL ON
// HOW BEST TO DO THIS.

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/agreement/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openCreateModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.openEndModal = (txHash, outputIndex) => {
        console.log(txHash, outputIndex);
        const modalInstance = $uibModal.open({
            templateUrl: 'endAgreementAppModal.html',
            controller: 'ModalEndAgreementCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                txHash: () => txHash,
                outputIndex: () => outputIndex
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.getIOUs = () => $http.get(apiBaseURL + "legal-agreements")
        .then((response) => demoApp.agreements = Object.keys(response.data)
            .map((key) => ({ state: response.data[key].state.data, ref: response.data[key].ref }))
            .reverse());

    demoApp.getMyIOUs = () => $http.get(apiBaseURL + "my-ious")
        .then((response) => demoApp.myious = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getIOUs();
    demoApp.getMyIOUs();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validate and create IOU.
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const createLegalAgreementEndpoint = `${apiBaseURL}create-legal-agreement?partyA=${modalInstance.form.partyA}&partyB=${modalInstance.form.partyB}&oracle=${modalInstance.form.oracle}&quantity=${modalInstance.form.quantity}&currency=${modalInstance.form.currency}`;

            // Create PO and handle success / fail responses.
            $http.post(createLegalAgreementEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                    demoApp.getIOUs();
                    demoApp.getMyIOUs();
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create IOU modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return (modalInstance.form.partyA === undefined) ||
            (modalInstance.form.partyB === undefined) ||
            (modalInstance.form.oracle === undefined) ||
            isNaN(modalInstance.form.quantity) ||
            (modalInstance.form.currency === undefined);
    }
});

app.controller('ModalEndAgreementCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, txHash, outputIndex) {
    const modalInstance = this;

    modalInstance.form = {
        txHash: txHash,
        outputIndex: outputIndex
    };
    modalInstance.formError = false;

    // Validate and create IOU.
    modalInstance.end = () => {
        console.log("Entered in end");
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const endAgreementEndpoint = `${apiBaseURL}end-agreement?txHash=${modalInstance.form.txHash}&outputIndex=${modalInstance.form.outputIndex}`;

        $http.put(endAgreementEndpoint).then(
            (result) => {
                modalInstance.displayMessage(result);
                demoApp.getIOUs();
                demoApp.getMyIOUs();
            },
            (result) => {
                modalInstance.displayMessage(result);
            }
        );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create IOU modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return (modalInstance.form.txHash === undefined) ||
            isNaN(modalInstance.form.outputIndex);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});
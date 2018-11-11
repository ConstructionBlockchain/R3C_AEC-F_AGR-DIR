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
    demoApp.peers = [];

    $http.get(apiBaseURL + "me")
    .then(response => {
        demoApp.thisNode = response.data.me;
        demoApp.oracle = demoApp.thisNode;
        return $http.get(apiBaseURL + "peers");
    })
    .then(response => {
        demoApp.peers = response.data.peers;
        return demoApp.getBustParties(demoApp.oracle);
    });

    demoApp.openCreateModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => demoApp.peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.openEndModal = (txHash, outputIndex) => {
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

    demoApp.openDirectModal = (txHash, outputIndex) => {
        const modalInstance = $uibModal.open({
            templateUrl: 'directAgreementAppModal.html',
            controller: 'ModalDirectAgreementCtrl',
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

    demoApp.openSetBustModal = () => {
        const everyOne = [];
        demoApp.peers.forEach(peer => everyOne.push(peer));
        everyOne.push(demoApp.thisNode);
        const modalInstance = $uibModal.open({
            templateUrl: 'setBustModal.html',
            controller: 'ModalSetBustInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => everyOne
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.getAgreements = () => $http.get(apiBaseURL + "legal-agreements")
        .then(response => demoApp.agreements = Object.keys(response.data)
            .map(key => ({ state: response.data[key].state.data, ref: response.data[key].ref }))
            .reverse());

    demoApp.getBustParties = (oracle) => $http.get(`${apiBaseURL}get-bust-parties?oracle=${oracle}`)
        .then(response => demoApp.bustParties = response.data);

    demoApp.getAgreements();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validate and create Agreement.
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
                    demoApp.getAgreements();
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

    // Close create Agreement modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Agreement.
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

    // Validate and end Agreement.
    modalInstance.end = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const endAgreementEndpoint = `${apiBaseURL}end-agreement?txHash=${modalInstance.form.txHash}&outputIndex=${modalInstance.form.outputIndex}`;

        $http.put(endAgreementEndpoint).then(
            (result) => {
                modalInstance.displayMessage(result);
                demoApp.getAgreements();
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

    // Close end agreement modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the agreement.
    function invalidFormInput() {
        return (modalInstance.form.txHash === undefined) ||
            isNaN(modalInstance.form.outputIndex);
    }
});

app.controller('ModalDirectAgreementCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, txHash, outputIndex) {
    const modalInstance = this;

    modalInstance.form = {
        txHash: txHash,
        outputIndex: outputIndex
    };
    modalInstance.formError = false;

    // Validate and do direct agreement.
    modalInstance.goDirect = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const directAgreementEndpoint = `${apiBaseURL}go-direct-agreement?txHash=${modalInstance.form.txHash}&outputIndex=${modalInstance.form.outputIndex}`;

        $http.put(directAgreementEndpoint).then(
            (result) => {
                modalInstance.displayMessage(result);
                demoApp.getAgreements();
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

    // Close go direct agreement modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the agreement.
    function invalidFormInput() {
        return (modalInstance.form.txHash === undefined) ||
            isNaN(modalInstance.form.outputIndex);
    }
});

app.controller('ModalSetBustInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validate and set bust party.
    modalInstance.setBust = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const setBustEndpoint = `${apiBaseURL}set-party-bust?party=${modalInstance.form.party}&isBust=${modalInstance.form.isBust}`;

            // Create PO and handle success / fail responses.
            $http.put(setBustEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                    demoApp.getBustParties(demoApp.oracle)
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

    // Close set bust party modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the bust party.
    function invalidFormInput() {
        return (modalInstance.form.party === undefined) ||
            (modalInstance.form.isBust === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});